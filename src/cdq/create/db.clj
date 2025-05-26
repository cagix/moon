(ns cdq.create.db
  (:require [cdq.db :as db]
            [cdq.g :as g]))

(def ^:private -k :ctx/db)

(defn add-db [ctx config]
  {:pre [(not (contains? ctx -k))]}
  (assoc ctx -k (db/create (:db config))))

(extend-type gdl.application.Context
  g/Database
  (get-raw [ctx property-id]
    (db/get-raw (-k ctx) property-id))

  (build [ctx property-id]
    (db/build (-k ctx) property-id ctx))

  (build-all [ctx property-type]
    (db/build-all (-k ctx) property-type ctx))

  (property-types [ctx]
    (db/property-types (-k ctx)))

  (schemas [ctx]
    (:schemas (-k ctx)))

  (update-property! [ctx property]
    (update ctx -k db/update! property))

  (delete-property! [ctx property-id]
    (update ctx -k db/delete! property-id)))
