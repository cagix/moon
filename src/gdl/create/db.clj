(ns gdl.create.db
  (:require [gdl.db]
            [cdq.db-impl :as db]))

(def ^:private -k :ctx/db)

(defn do! [{:keys [ctx/config]
            :as ctx}]
  (extend (class ctx)
    gdl.db/Database
    {:property-types (fn [ctx]
                       (db/property-types (-k ctx)))
     :schemas (fn [ctx]
                (:schemas (-k ctx)))
     :get-raw (fn [ctx property-id]
                (db/get-raw (-k ctx) property-id))
     :all-raw (fn [ctx property-type]
                (db/all-raw (-k ctx) property-type))
     :build (fn [ctx property-id]
              (db/build (-k ctx) property-id ctx))
     :build-all (fn [ctx property-type]
                  (db/build-all (-k ctx) property-type ctx))
     :update-property (fn [ctx property]
                        (update ctx -k db/update! property))
     :delete-property (fn [ctx property-id]
                        (update ctx -k db/delete! property-id))})
  (assoc ctx -k (db/create (:db config))))
