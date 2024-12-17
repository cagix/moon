(ns dev
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.utils :refer [recur-sort-map async-pprint-spit!]]
            [malli.core :as m]))

; Requirements! / Tests !
; * maybe the error message is unreadable if the whole db is one map ... ?
; * why don't I use an existing db (datomic?)

(def schema
  [:map-of :keyword :int])

[:s/map
 [:properties/audiovisuals
  :properties/creatures
  :properties/items
  :properties/projectiles
  :properties/skills
  :properties/worlds]]

(m/validate schema {:a 1
                    :b 2
                    :c 3
                    })

(comment
 (def db-data (-> "properties.edn"
                  io/resource
                  slurp
                  edn/read-string))

 (println (count db-data))
 ; 372 properties

 (defn- property-type [{:keys [property/id]}]
   (keyword "properties" (namespace id)))

 (for [[ptype properties] (group-by property-type db-data)]
   [ptype (count properties)])

 #_([:properties/audiovisuals 6]
    [:properties/creatures 128]
    [:properties/items 223]
    [:properties/projectiles 2]
    [:properties/skills 10]
    [:properties/worlds 3])

 (defn- async-write-to-file! [m]
   (async-pprint-spit! (io/resource "properties.edn")
                       (recur-sort-map m)))

 ;(async-write-to-file! new-db-data)

 (def new-db-data
   (into {}
         (for [[ptype properties] (group-by property-type db-data)]
           [ptype (into {}
                        (for [{:keys [property/id] :as property} properties]
                          [id (dissoc property :property/id)]))])))

 (keys new-db-data)
 (:properties/audiovisuals :properties/creatures :properties/items :properties/projectiles :properties/skills :properties/worlds)

 (for [[ptype id-map] new-db-data]
   [ptype (count id-map)])

 (:creatures/vampire (:properties/creatures new-db-data)))

;;;;;;


; property-type remove
; make db a map of 'different types' to 'id-property' map

; so The whole DB as one schema?

[:s/map
 [:properties/audiovisuals
  :properties/creatures
  :properties/items
  :properties/projectiles
  :properties/skills
  :properties/worlds]]


; and properties is map-of
; id to audiovisuals properties

; so then properties/audiovisuals is just the propertiess
