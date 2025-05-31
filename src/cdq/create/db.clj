(ns cdq.create.db
  (:require [cdq.db :as db]
            [cdq.g :as g]))

(def ^:private -k :ctx/db)

(defn do! [{:keys [ctx/config]
            :as ctx}]
  (extend (class ctx)
    g/Database
    {:property-types (fn [ctx]
                       (db/property-types (-k ctx)))
     :schemas (fn [ctx]
                (:schemas (-k ctx)))
     :get-raw (fn [ctx property-id]
                (db/get-raw (-k ctx) property-id))
     :build (fn [ctx property-id]
              (db/build (-k ctx) property-id ctx))
     :build-all (fn [ctx property-type]
                  (db/build-all (-k ctx) property-type ctx))
     :update-property (fn [ctx property]
                        (update ctx -k db/update! property))
     :delete-property (fn [ctx property-id]
                        (update ctx -k db/delete! property-id))})
  (assoc ctx -k (db/create (:db config))))
