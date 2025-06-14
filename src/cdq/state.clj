(ns cdq.state)

(defmulti enter! (fn [[k] _eid]
                  k))
(defmethod enter! :default [_ _eid])

(defmulti exit! (fn [[k] _eid _ctx]
                  k))
(defmethod exit! :default [_ _eid _ctx])
