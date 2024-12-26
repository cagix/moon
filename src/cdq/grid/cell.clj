(ns cdq.grid.cell)

(defprotocol Cell
  (blocked? [cell z-order])
  (blocks-vision? [cell])
  (occupied-by-other? [cell eid]
                      "returns true if there is some occupying body with center-tile = this cell
                      or a multiple-cell-size body which touches this cell.")
  (nearest-entity          [cell faction])
  (nearest-entity-distance [cell faction]))
