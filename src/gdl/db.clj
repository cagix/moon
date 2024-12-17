(ns gdl.db ; g.o.d. = game object database ; - not property but 'object' !
  ; !build-object! 'build right begriff? ask chatgpt...'
  ; spritesheets/tetures/fonts.../cursors?!?
  (:require [gdl.db-ctx-free :as db]
            [gdl.graphics.animation :as animation]
            [gdl.graphics.sprite :as sprite]
            [gdl.schema :as schema :refer [edn->value malli-form]]
            [gdl.malli :as m]
            [gdl.utils :refer [defmethods]]))

(declare ^:private db-map)

(defn property-types []
  (db/property-types db-map))

(defn schema-of [k]
  (db/schema-of db-map k))

(defn update! [property]
  (alter-var-root #'db-map db/update property)
  (db/async-write-to-file! db-map))

(defn delete! [property-id]
  (alter-var-root #'db-map db/delete property-id)
  (db/async-write-to-file! db-map))

(defn get-raw [id]
  (db/get-raw db-map id))

(defn all-raw [property-type]
  (db/all-raw db-map property-type))

(defn build [id]
  (db/build db-map id))

(defn build-all [property-type]
  (db/build-all db-map property-type))

(defn migrate [property-type update-fn]
  (doseq [id (map :property/id (all-raw property-type))]
    (println id)
    (alter-var-root #'db-map update :db-data update id update-fn))
  (db/async-write-to-file! db-map))

(defn setup [config]
  (def db-map (db/create config)))

(defmethod malli-form :s/val-max [_ _schemas] m/val-max-schema)
(defmethod malli-form :s/number  [_ _schemas] m/number-schema)
(defmethod malli-form :s/nat-int [_ _schemas] m/nat-int-schema)
(defmethod malli-form :s/int     [_ _schemas] m/int-schema)
(defmethod malli-form :s/pos     [_ _schemas] m/pos-schema)
(defmethod malli-form :s/pos-int [_ _schemas] m/pos-int-schema)
(defmethod malli-form :s/sound   [_ _schemas] m/string-schema)

(defn- edn->sprite [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (sprite/from-sheet (sprite/sheet file tilew tileh)
                         [(int (/ sprite-x tilew))
                          (int (/ sprite-y tileh))]))
    (sprite/create file)))

(defmethods :s/image
  (malli-form  [_ _schemas]
    m/image-schema)

  (edn->value [_ edn]
    (edn->sprite edn)))

(defmethods :s/animation
  (malli-form [_ _schemas]
    m/animation-schema)

  (edn->value [_ {:keys [frames frame-duration looping?]}]
    (animation/create (map edn->sprite frames)
                      :frame-duration frame-duration
                      :looping? looping?)))

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(defmethods :s/one-to-one
  (malli-form [[_ property-type] _schemas]
    (m/qualified-keyword-schema (type->id-namespace property-type)))
  (edn->value [_ property-id]
    (build property-id)))

(defmethods :s/one-to-many
  (malli-form [[_ property-type] _schemas]
    (m/set-schema (m/qualified-keyword-schema (type->id-namespace property-type))))
  (edn->value [_ property-ids]
    (set (map build property-ids))))

(defn- map-form [ks schemas]
  (m/map-schema ks (fn [k]
                     (malli-form (schema/schema-of schemas k)
                                 schemas))))
; TODO schema/validate comes to this...
; but db-data is not yet existing?

(defmethod malli-form :s/map [[_ ks] schemas]
  (map-form ks schemas))

(defmethod malli-form :s/map-optional [[_ ks] schemas]
  (map-form (map (fn [k] [k {:optional true}]) ks)
            schemas))

(defn- namespaced-ks [schemas ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys schemas)))

(defmethod malli-form :s/components-ns [[_ ns-name-k] schemas]
  (malli-form [:s/map-optional (namespaced-ks schemas ns-name-k)]
              schemas))
