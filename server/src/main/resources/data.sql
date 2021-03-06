CREATE TABLE IF NOT EXISTS "users" (
	"raw_id" INTEGER NOT NULL,
	"uuid" TEXT NOT NULL,
	"email"	TEXT NOT NULL,
	"nickname" TEXT NOT NULL,
	PRIMARY KEY("raw_id" AUTOINCREMENT),
	UNIQUE("uuid"),
	UNIQUE("email")
);
CREATE TABLE IF NOT EXISTS "passwords" (
	"raw_id" INTEGER NOT NULL,
	"uuid" TEXT NOT NULL,
	"password" TEXT NOT NULL,
	FOREIGN KEY("uuid") REFERENCES "users"("uuid") ON UPDATE RESTRICT,
	PRIMARY KEY("raw_id" AUTOINCREMENT)
);