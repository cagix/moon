(ns cdq.application-state
  (:require [cdq.db :as db]
            [cdq.g :as g]
            [cdq.schemas :as schemas]
            [cdq.schemas-impl :as schemas-impl]
            [cdq.utils :as utils]
            [gdl.application]))

(defn- create-schemas [path]
  (schemas-impl/create (utils/io-slurp-edn path)))

(defn create! [config]
  (-> (gdl.application/create-state! config)
      (utils/safe-merge {:ctx/config config
                         :ctx/db (db/create {:schemas (create-schemas (:schemas config))
                                             :properties (:properties config)})})
      ((requiring-resolve 'cdq.game-state/create!))))

(extend-type gdl.application.Context
  g/Config
  (config [{:keys [ctx/config]} key]
    (get config key)))

(extend-type gdl.application.Context
  g/Database
  (get-raw [{:keys [ctx/db]} property-id]
    (db/get-raw db property-id))

  (build [{:keys [ctx/db] :as ctx} property-id]
    (db/build db property-id ctx))

  (build-all [{:keys [ctx/db] :as ctx} property-type]
    (db/build-all db property-type ctx))

  (property-types [{:keys [ctx/db]}]
    (schemas/property-types (:schemas db)))

  (schemas [{:keys [ctx/db]}]
    (:schemas db))

  (update-property! [ctx property]
    (update ctx :ctx/db db/update! property))

  (delete-property! [ctx property-id]
    (update ctx :ctx/db db/delete! property-id)))
