(ns cdq.context)

(defmulti add-entity (fn [[k] _eid]
                       k))
(defmethod add-entity :default [_ _])

(defmulti remove-entity (fn [[k] _eid]
                          k))
(defmethod remove-entity :default [_ _])

(defmulti position-changed (fn [[k] _eid]
                             k))
(defmethod position-changed :default [_ _])
