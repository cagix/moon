(ns cdq.create.db
  (:require [cdq.db :as db]
            [cdq.g :as g]))

(def ^:private -k :ctx/db)

(defn add-db [ctx config]
  {:pre [(not (contains? ctx -k))]}
  (extend (class ctx)
    g/Database
    {:get-raw (fn [ctx property-id]
                (db/get-raw (-k ctx) property-id))
     :build (fn [ctx property-id]
              (db/build (-k ctx) property-id ctx))
     :build-all (fn [ctx property-type]
                  (db/build-all (-k ctx) property-type ctx))
     :property-types (fn [ctx]
                       (db/property-types (-k ctx)))
     :schemas (fn [ctx]
                (:schemas (-k ctx)))
     :update-property! (fn [ctx property]
                         (update ctx -k db/update! property))
     :delete-property! (fn [ctx property-id]
                         (update ctx -k db/delete! property-id))})
  (assoc ctx -k (db/create (:db config))))


