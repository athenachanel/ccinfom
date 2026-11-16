#ifndef CONCERT_H
#define CONCERT_H

/*
 * APPLICATION TIER (logic) for Concert/Event Record Management
 * and the Venue Utilization Report.
 * The UI calls these functions. The UI never writes SQL.
 */

#define CONCERT_OK          0
#define CONCERT_NOT_FOUND   1
#define CONCERT_ERROR      -1

#define MAX_SUPPLIERS_PER_CONCERT 10

typedef struct {
    int  id;
    char name[151];
    int  artist_id;
    char artist_name[101];     /* filled when reading, ignored when saving */
    int  venue_id;
    char venue_name[101];      /* filled when reading, ignored when saving */
    char date[11];             /* YYYY-MM-DD */
    char start_time[9];        /* HH:MM:SS */
    char end_time[9];          /* HH:MM:SS */
    int  ticket_capacity;

    /* Used only by concert_add(): every concert needs at least one supplier (UML 1..*). */
    int  supplier_ids[MAX_SUPPLIERS_PER_CONCERT];
    int  supplier_count;
} Concert;

typedef struct {
    char venue_name[101];
    char city[61];
    int  concerts_held;
    long total_ticket_capacity;
} VenueUtilRow;

/* Message explaining the most recent CONCERT_ERROR / CONCERT_NOT_FOUND. */
const char *concert_last_error(void);

/* Record management. Each returns CONCERT_OK, CONCERT_NOT_FOUND or CONCERT_ERROR. */
int concert_add(Concert *c, int *new_id);   /* also links c->supplier_ids to the concert */
int concert_update(Concert *c);             /* uses c->id to find the record */
int concert_delete(int id);
int concert_get(int id, Concert *out);

/* Listing. Return the number of rows filled in (0 or more), or CONCERT_ERROR. */
int concert_list_all(Concert *out, int max);
int concert_list_by_month_year(int month, int year, Concert *out, int max);

/* Report: per venue, concerts held and total ticket capacity for a month and year.
   Returns the number of rows filled in, or CONCERT_ERROR. */
int report_venue_utilization(int month, int year, VenueUtilRow *out, int max);

#endif
