(ns cdq.state)

(defmulti cursor (fn [[k]]
                   k))
(defmethod cursor :default [_])

(defmulti pause-game? (fn [[k]]
                        k))

(defmulti enter! (fn [[k] _eid]
                  k))
(defmethod enter! :default [_ _eid])

(defmulti exit! (fn [[k] _eid _ctx]
                  k))
(defmethod exit! :default [_ _eid _ctx])

(defmulti clicked-inventory-cell (fn [[k] _eid _cell]
                                   k))
(defmethod clicked-inventory-cell :default [_ _eid _cell])

(defmulti manual-tick (fn [[k] _eid ctx]
                        k))
(defmethod manual-tick :default [_ _eid ctx])

(defmulti draw-gui-view (fn [[k] _eid ctx]
                          k))
(defmethod draw-gui-view :default [_ _eid ctx])
