ALTER TABLE FORELDREANSVAR
    ADD CONSTRAINT FK_BARN_IDENT FOREIGN KEY (barn_ident) REFERENCES BRUKER_DATA_BARN (barn_ident)  ON UPDATE CASCADE;