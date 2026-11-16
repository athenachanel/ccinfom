#include "concert.h"
#include "db.h"

#include <ctype.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static char g_error[256] = "";

static void set_error(const char *fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(g_error, sizeof g_error, fmt, ap);
    va_end(ap);
}

const char *concert_last_error(void)
{
    return g_error;
}

/* ---------- small helpers ---------- */

static int is_blank(const char *s)
{
    while (*s) {
        if (!isspace((unsigned char)*s)) {
            return 0;
        }
        s++;
    }
    return 1;
}

static int days_in_month(int y, int m)
{
    static const int days[12] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    int leap = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0);
    return (m == 2 && leap) ? 29 : days[m - 1];
}

/* Accepts only a real date written as YYYY-MM-DD. */
static int valid_date(const char *s)
{
    int y, m, d, i;
    if (strlen(s) != 10 || s[4] != '-' || s[7] != '-') {
        return 0;
    }
    for (i = 0; i < 10; i++) {
        if (i != 4 && i != 7 && !isdigit((unsigned char)s[i])) {
            return 0;
        }
    }
    if (sscanf(s, "%4d-%2d-%2d", &y, &m, &d) != 3) {
        return 0;
    }
    return y >= 1900 && y <= 2100 && m >= 1 && m <= 12 && d >= 1 && d <= days_in_month(y, m);
}

/* Turns H:MM, HH:MM or HH:MM:SS into HH:MM:SS. out must hold 9 bytes. */
static int normalize_time(const char *in, char *out)
{
    int h, m, sec = 0, n;
    const char *p;
    for (p = in; *p; p++) {
        if (!isdigit((unsigned char)*p) && *p != ':') {
            return 0;
        }
    }
    n = sscanf(in, "%d:%d:%d", &h, &m, &sec);
    if (n < 2 || h < 0 || h > 23 || m < 0 || m > 59 || sec < 0 || sec > 59) {
        return 0;
    }
    snprintf(out, 9, "%02d:%02d:%02d", h, m, sec);
    return 1;
}

static int valid_month_year(int month, int year)
{
    if (month < 1 || month > 12) {
        set_error("Month must be from 1 to 12.");
        return 0;
    }
    if (year < 1900 || year > 2100) {
        set_error("Year must be between 1900 and 2100.");
        return 0;
    }
    return 1;
}

/* Runs a query expected to return one number in its first column.
   Returns 0 when read, 1 when there is no row, -1 on database error. */
static int fetch_long(const char *sql, long *out)
{
    MYSQL_RES *res = db_select(sql);
    MYSQL_ROW row;
    if (!res) {
        set_error("Database error: %s", db_error());
        return -1;
    }
    row = mysql_fetch_row(res);
    if (!row || !row[0]) {
        mysql_free_result(res);
        return 1;
    }
    *out = atol(row[0]);
    mysql_free_result(res);
    return 0;
}

static void copy_str(char *dst, size_t size, const char *src)
{
    snprintf(dst, size, "%s", src ? src : "");
}

/* ---------- validation ---------- */

static int validate_concert(Concert *c)
{
    char sql[256];
    char start[9], end[9];
    long value;
    int rc;

    if (is_blank(c->name)) {
        set_error("Concert name cannot be empty.");
        return -1;
    }
    if (!valid_date(c->date)) {
        set_error("Date must be a real date written as YYYY-MM-DD.");
        return -1;
    }
    if (!normalize_time(c->start_time, start)) {
        set_error("Start time must look like HH:MM (24-hour), for example 19:30.");
        return -1;
    }
    if (!normalize_time(c->end_time, end)) {
        set_error("End time must look like HH:MM (24-hour), for example 22:00.");
        return -1;
    }
    /* Simple rule: a concert ends on the same day it starts. */
    if (strcmp(end, start) <= 0) {
        set_error("End time must be later than the start time.");
        return -1;
    }
    memcpy(c->start_time, start, sizeof start);
    memcpy(c->end_time, end, sizeof end);

    if (c->ticket_capacity <= 0) {
        set_error("Ticket capacity must be greater than 0.");
        return -1;
    }

    snprintf(sql, sizeof sql, "SELECT 1 FROM artist WHERE artist_id = %d", c->artist_id);
    rc = fetch_long(sql, &value);
    if (rc < 0) {
        return -1;
    }
    if (rc == 1) {
        set_error("Artist ID %d does not exist.", c->artist_id);
        return -1;
    }

    snprintf(sql, sizeof sql, "SELECT venue_capacity FROM venue WHERE venue_id = %d", c->venue_id);
    rc = fetch_long(sql, &value);
    if (rc < 0) {
        return -1;
    }
    if (rc == 1) {
        set_error("Venue ID %d does not exist.", c->venue_id);
        return -1;
    }
    if (c->ticket_capacity > value) {
        set_error("Ticket capacity (%d) cannot be more than the venue's capacity (%ld).",
                  c->ticket_capacity, value);
        return -1;
    }
    return 0;
}

/* A concert must have at least one supplier (UML 1..*), each one real and listed once. */
static int validate_suppliers(const Concert *c)
{
    char sql[256];
    long value;
    int i, j, rc;

    if (c->supplier_count < 1) {
        set_error("A concert needs at least one supplier.");
        return -1;
    }
    if (c->supplier_count > MAX_SUPPLIERS_PER_CONCERT) {
        set_error("A concert can have at most %d suppliers here.", MAX_SUPPLIERS_PER_CONCERT);
        return -1;
    }
    for (i = 0; i < c->supplier_count; i++) {
        for (j = 0; j < i; j++) {
            if (c->supplier_ids[i] == c->supplier_ids[j]) {
                set_error("Supplier ID %d is listed more than once.", c->supplier_ids[i]);
                return -1;
            }
        }
        snprintf(sql, sizeof sql, "SELECT 1 FROM supplier WHERE supplier_id = %d", c->supplier_ids[i]);
        rc = fetch_long(sql, &value);
        if (rc < 0) {
            return -1;
        }
        if (rc == 1) {
            set_error("Supplier ID %d does not exist.", c->supplier_ids[i]);
            return -1;
        }
    }
    return 0;
}

/* ---------- reading ---------- */

#define CONCERT_SELECT \
    "SELECT c.concert_id, c.concert_name, c.artist_id, a.artist_name, " \
    "c.venue_id, v.venue_name, c.concert_date, c.start_time, c.end_time, c.ticket_capacity " \
    "FROM concert c " \
    "JOIN artist a ON a.artist_id = c.artist_id " \
    "JOIN venue v ON v.venue_id = c.venue_id"

static void row_to_concert(MYSQL_ROW row, Concert *c)
{
    memset(c, 0, sizeof *c);
    c->id = atoi(row[0]);
    copy_str(c->name, sizeof c->name, row[1]);
    c->artist_id = atoi(row[2]);
    copy_str(c->artist_name, sizeof c->artist_name, row[3]);
    c->venue_id = atoi(row[4]);
    copy_str(c->venue_name, sizeof c->venue_name, row[5]);
    copy_str(c->date, sizeof c->date, row[6]);
    copy_str(c->start_time, sizeof c->start_time, row[7]);
    copy_str(c->end_time, sizeof c->end_time, row[8]);
    c->ticket_capacity = atoi(row[9]);
}

static int list_where(const char *where, Concert *out, int max)
{
    char sql[1536];
    MYSQL_RES *res;
    MYSQL_ROW row;
    int n = 0;

    snprintf(sql, sizeof sql, "%s %s ORDER BY c.concert_date, c.start_time, c.concert_id",
             CONCERT_SELECT, where);
    res = db_select(sql);
    if (!res) {
        set_error("Database error: %s", db_error());
        return CONCERT_ERROR;
    }
    while (n < max && (row = mysql_fetch_row(res)) != NULL) {
        row_to_concert(row, &out[n]);
        n++;
    }
    mysql_free_result(res);
    return n;
}

int concert_get(int id, Concert *out)
{
    char where[64];
    int n;
    snprintf(where, sizeof where, "WHERE c.concert_id = %d", id);
    n = list_where(where, out, 1);
    if (n < 0) {
        return CONCERT_ERROR;
    }
    if (n == 0) {
        set_error("No concert with ID %d.", id);
        return CONCERT_NOT_FOUND;
    }
    return CONCERT_OK;
}

int concert_list_all(Concert *out, int max)
{
    return list_where("", out, max);
}

int concert_list_by_month_year(int month, int year, Concert *out, int max)
{
    char where[160];
    if (!valid_month_year(month, year)) {
        return CONCERT_ERROR;
    }
    snprintf(where, sizeof where,
             "WHERE c.concert_date BETWEEN '%04d-%02d-01' AND LAST_DAY('%04d-%02d-01')",
             year, month, year, month);
    return list_where(where, out, max);
}

/* ---------- writing ---------- */

int concert_add(Concert *c, int *new_id)
{
    char sql[1024];
    char name[311];
    long id;
    int i;

    if (validate_concert(c) != 0 || validate_suppliers(c) != 0) {
        return CONCERT_ERROR;
    }
    db_escape(name, sizeof name, c->name);

    /* One transaction: the concert and its supplier links are saved together or not at all. */
    if (db_execute("START TRANSACTION") != 0) {
        set_error("Database error: %s", db_error());
        return CONCERT_ERROR;
    }
    snprintf(sql, sizeof sql,
             "INSERT INTO concert (concert_name, artist_id, venue_id, concert_date, "
             "start_time, end_time, ticket_capacity) VALUES ('%s', %d, %d, '%s', '%s', '%s', %d)",
             name, c->artist_id, c->venue_id, c->date, c->start_time, c->end_time, c->ticket_capacity);
    if (db_execute(sql) != 0) {
        set_error("Database error: %s", db_error());
        db_execute("ROLLBACK");
        return CONCERT_ERROR;
    }
    id = (long)db_last_insert_id();
    for (i = 0; i < c->supplier_count; i++) {
        snprintf(sql, sizeof sql,
                 "INSERT INTO concert_supplier (concert_id, supplier_id) VALUES (%ld, %d)",
                 id, c->supplier_ids[i]);
        if (db_execute(sql) != 0) {
            set_error("Database error: %s", db_error());
            db_execute("ROLLBACK");
            return CONCERT_ERROR;
        }
    }
    if (db_execute("COMMIT") != 0) {
        set_error("Database error: %s", db_error());
        return CONCERT_ERROR;
    }
    c->id = (int)id;
    if (new_id) {
        *new_id = (int)id;
    }
    return CONCERT_OK;
}

int concert_update(Concert *c)
{
    char sql[1024];
    char name[311];
    long value;
    int rc;

    snprintf(sql, sizeof sql, "SELECT 1 FROM concert WHERE concert_id = %d", c->id);
    rc = fetch_long(sql, &value);
    if (rc < 0) {
        return CONCERT_ERROR;
    }
    if (rc == 1) {
        set_error("No concert with ID %d.", c->id);
        return CONCERT_NOT_FOUND;
    }
    if (validate_concert(c) != 0) {
        return CONCERT_ERROR;
    }
    db_escape(name, sizeof name, c->name);
    snprintf(sql, sizeof sql,
             "UPDATE concert SET concert_name = '%s', artist_id = %d, venue_id = %d, "
             "concert_date = '%s', start_time = '%s', end_time = '%s', ticket_capacity = %d "
             "WHERE concert_id = %d",
             name, c->artist_id, c->venue_id, c->date, c->start_time, c->end_time,
             c->ticket_capacity, c->id);
    if (db_execute(sql) != 0) {
        set_error("Database error: %s", db_error());
        return CONCERT_ERROR;
    }
    return CONCERT_OK;
}

int concert_delete(int id)
{
    char sql[256];
    long purchases;
    int rc;

    snprintf(sql, sizeof sql, "SELECT 1 FROM concert WHERE concert_id = %d", id);
    rc = fetch_long(sql, &purchases);
    if (rc < 0) {
        return CONCERT_ERROR;
    }
    if (rc == 1) {
        set_error("No concert with ID %d.", id);
        return CONCERT_NOT_FOUND;
    }

    /* Sold tickets are sales records, so a concert that has any cannot be deleted. */
    snprintf(sql, sizeof sql, "SELECT COUNT(*) FROM ticket_purchase WHERE concert_id = %d", id);
    if (fetch_long(sql, &purchases) != 0) {
        return CONCERT_ERROR;
    }
    if (purchases > 0) {
        set_error("This concert has %ld ticket purchase record(s), so it cannot be deleted.", purchases);
        return CONCERT_ERROR;
    }

    /* The supplier links belong to the concert, so they are removed with it. */
    if (db_execute("START TRANSACTION") != 0) {
        set_error("Database error: %s", db_error());
        return CONCERT_ERROR;
    }
    snprintf(sql, sizeof sql, "DELETE FROM concert_supplier WHERE concert_id = %d", id);
    if (db_execute(sql) != 0) {
        set_error("Database error: %s", db_error());
        db_execute("ROLLBACK");
        return CONCERT_ERROR;
    }
    snprintf(sql, sizeof sql, "DELETE FROM concert WHERE concert_id = %d", id);
    if (db_execute(sql) != 0) {
        set_error("Database error: %s", db_error());
        db_execute("ROLLBACK");
        return CONCERT_ERROR;
    }
    if (db_execute("COMMIT") != 0) {
        set_error("Database error: %s", db_error());
        return CONCERT_ERROR;
    }
    return CONCERT_OK;
}

/* ---------- report ---------- */

int report_venue_utilization(int month, int year, VenueUtilRow *out, int max)
{
    char sql[1024];
    MYSQL_RES *res;
    MYSQL_ROW row;
    int n = 0;

    if (!valid_month_year(month, year)) {
        return CONCERT_ERROR;
    }
    snprintf(sql, sizeof sql,
             "SELECT v.venue_name, v.city, COUNT(c.concert_id), COALESCE(SUM(c.ticket_capacity), 0) "
             "FROM venue v "
             "LEFT JOIN concert c ON c.venue_id = v.venue_id "
             "AND c.concert_date BETWEEN '%04d-%02d-01' AND LAST_DAY('%04d-%02d-01') "
             "GROUP BY v.venue_id, v.venue_name, v.city "
             "ORDER BY v.venue_name",
             year, month, year, month);
    res = db_select(sql);
    if (!res) {
        set_error("Database error: %s", db_error());
        return CONCERT_ERROR;
    }
    while (n < max && (row = mysql_fetch_row(res)) != NULL) {
        copy_str(out[n].venue_name, sizeof out[n].venue_name, row[0]);
        copy_str(out[n].city, sizeof out[n].city, row[1]);
        out[n].concerts_held = atoi(row[2]);
        out[n].total_ticket_capacity = atol(row[3]);
        n++;
    }
    mysql_free_result(res);
    return n;
}
