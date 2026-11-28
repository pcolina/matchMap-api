# First administrator

This page explains how to give the `ADMIN` role to an account. The role lets an account change the geographic catalog of countries and cities. Only an operator with access to the database can do this. The API has no operation that grants the role, so nobody can make themselves an administrator through the app.

## Before you start

1. Create the account first, with the normal sign-up, so that it exists in the database.
2. Write the email in lowercase and without spaces. The API stores emails in lowercase.
3. Make sure you have access to the database of the environment you are working on.

## Give the role

Run this command, replacing `<email>` with the email of the account. Keep the quotes around the email.

```sql
UPDATE user_account SET role = 'ADMIN' WHERE email = '<email>';
```

On the local development environment, the database runs in Docker Compose. From the repository root, run this in PowerShell, with the same SQL inside the quotes:

```powershell
docker compose -f infra/docker/docker-compose.yml exec db psql -U matchmap -d matchmap -c "UPDATE user_account SET role = 'ADMIN' WHERE email = '<email>';"
```

Each environment has its own database. Run the same statement against that environment's database with the tool you use for it.

## Check the result

```sql
SELECT email, role FROM user_account WHERE email = '<email>';
```

The `role` column must show `ADMIN`. If the query returns no rows, the email does not match an account. Check the spelling and the lowercase rule.

## Undo the role

To return an account to a normal user, run the same kind of command with `'USER'`:

```sql
UPDATE user_account SET role = 'USER' WHERE email = '<email>';
```

## Notes

- Only give the role to people who should change the catalog. An administrator can change every country and city.
- Record who received the role and when. The database does not keep that history.
