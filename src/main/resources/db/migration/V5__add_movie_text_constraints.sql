ALTER TABLE movies
    ADD CONSTRAINT chk_movie_title_not_blank CHECK (btrim(title) <> ''),
    ADD CONSTRAINT chk_movie_description_not_blank CHECK (btrim(description) <> '');
