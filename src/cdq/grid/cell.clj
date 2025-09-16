(ns cdq.grid.cell)

(defprotocol Cell
  (blocked? [_ z-order])
  (blocks-vision? [_])
  (occupied-by-other? [_ eid]
                      "returns true if there is some occupying body with center-tile = this cell
                      or a multiple-cell-size body which touches this cell.")
  (nearest-entity          [_ faction])
  (nearest-entity-distance [_ faction])
  (pf-blocked? [_]))

(defrecord FieldData [distance eid])

(defn add-field-data [cell faction distance eid]
  (assoc cell faction (->FieldData distance eid)))

(defn remove-field-data [cell faction]
  (assoc cell faction nil)) ; don't dissoc - will lose the Cell record type
