(ns cdq.create.db
  (:require [cdq.db :as db]
            [cdq.g :as g]))

(defn add-db [ctx config]
  {:pre [(not (contains? ctx :ctx/db))]}
  (assoc ctx :ctx/db (db/create (:db config))))

(extend-type gdl.application.Context
  g/Database
  (get-raw [{:keys [ctx/db]} property-id]
    (db/get-raw db property-id))

  (build [{:keys [ctx/db] :as ctx} property-id]
    (db/build db property-id ctx))

  (build-all [{:keys [ctx/db] :as ctx} property-type]
    (db/build-all db property-type ctx))

  (property-types [{:keys [ctx/db]}]
    (db/property-types db))

  (schemas [{:keys [ctx/db]}]
    (:schemas db))

  (update-property! [ctx property]
    (update ctx :ctx/db db/update! property))

  (delete-property! [ctx property-id]
    (update ctx :ctx/db db/delete! property-id)))
