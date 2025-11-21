"""initial migration

Revision ID: 0002
Create Date: 2023-10-31
"""
revision = '0002'
down_revision = '0001'
from alembic import op
import sqlalchemy as sa

def upgrade():
    # No-op placeholder migration (original migration is 0001)
    pass

def downgrade():
    pass