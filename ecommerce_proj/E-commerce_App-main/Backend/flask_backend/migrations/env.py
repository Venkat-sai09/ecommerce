from logging.config import fileConfig
from alembic import context
from flask import current_app

# Alembic config
config = context.config

# If an alembic.ini file exists, use it for logging config
if config.config_file_name is not None:
    try:
        fileConfig(config.config_file_name)
    except Exception:
        # ignore missing or malformed logging config in container environments
        pass

# Use the application's metadata for autogenerate support
target_metadata = current_app.extensions['migrate'].db.metadata


def run_migrations_offline():
    """Run migrations in 'offline' mode."""
    url = current_app.config.get('SQLALCHEMY_DATABASE_URI')
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
    )

    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online():
    """Run migrations in 'online' mode."""
    connectable = current_app.extensions['migrate'].db.engine

    with connectable.connect() as connection:
        context.configure(connection=connection, target_metadata=target_metadata)

        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
