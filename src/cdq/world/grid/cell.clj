(ns cdq.world.grid.cell)

(defprotocol Cell
  (blocked? [_ z-order])
  (blocks-vision? [_])
  (occupied-by-other? [_ eid]
                      "returns true if there is some occupying body with center-tile = this cell
                      or a multiple-cell-size body which touches this cell.")
  (nearest-entity          [_ faction])
  (nearest-entity-distance [_ faction]))

(defn pf-blocked? [cell]
  (blocked? cell :z-order/ground))
