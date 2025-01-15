(ns cdq.schemas
 (:require clojure.assets
           clojure.context
           clojure.db
           clojure.graphics.animation
           clojure.graphics.sprite
           clojure.malli
           clojure.schema
           clojure.utils))

(defmethod clojure.schema/malli-form :s/val-max [_ _schemas] clojure.malli/val-max-schema)
(defmethod clojure.schema/malli-form :s/number  [_ _schemas] clojure.malli/number-schema)
(defmethod clojure.schema/malli-form :s/nat-int [_ _schemas] clojure.malli/nat-int-schema)
(defmethod clojure.schema/malli-form :s/int     [_ _schemas] clojure.malli/int-schema)
(defmethod clojure.schema/malli-form :s/pos     [_ _schemas] clojure.malli/pos-schema)
(defmethod clojure.schema/malli-form :s/pos-int [_ _schemas] clojure.malli/pos-int-schema)

(clojure.utils/defcomponent :s/sound
  (clojure.schema/malli-form [_ _schemas]
    clojure.malli/string-schema)

  (clojure.db/edn->value [_ sound-name _db {:keys [clojure/assets]}]
    (clojure.assets/sound assets sound-name)))

(defn- edn->sprite [c {:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (clojure.graphics.sprite/from-sheet (clojure.graphics.sprite/sheet c
                                                                         file
                                                                         tilew
                                                                         tileh)
                                          [(int (/ sprite-x tilew))
                                           (int (/ sprite-y tileh))]
                                          c))
    (clojure.context/sprite c file)))

(clojure.utils/defcomponent :s/image
  (clojure.schema/malli-form  [_ _schemas]
    clojure.malli/image-schema)

  (clojure.db/edn->value [_ edn _db c]
    (edn->sprite c edn)))

(clojure.utils/defcomponent :s/animation
  (clojure.schema/malli-form [_ _schemas]
    clojure.malli/animation-schema)

  (clojure.db/edn->value [_ {:keys [frames frame-duration looping?]} _db c]
    (clojure.graphics.animation/create (map #(edn->sprite c %) frames)
                                   :frame-duration frame-duration
                                   :looping? looping?)))

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(clojure.utils/defcomponent :s/one-to-one
  (clojure.schema/malli-form [[_ property-type] _schemas]
    (clojure.malli/qualified-keyword-schema (type->id-namespace property-type)))
  (clojure.db/edn->value [_ property-id db c]
    (clojure.context/build c property-id)))

(clojure.utils/defcomponent :s/one-to-many
  (clojure.schema/malli-form [[_ property-type] _schemas]
    (clojure.malli/set-schema (clojure.malli/qualified-keyword-schema (type->id-namespace property-type))))
  (clojure.db/edn->value [_ property-ids db c]
    (set (map #(clojure.context/build c %) property-ids))))

(defn- map-form [ks schemas]
  (clojure.malli/map-schema ks (fn [k]
                             (clojure.schema/malli-form (clojure.schema/schema-of schemas k)
                                                    schemas))))

(defmethod clojure.schema/malli-form :s/map [[_ ks] schemas]
  (map-form ks schemas))

(defmethod clojure.schema/malli-form :s/map-optional [[_ ks] schemas]
  (map-form (map (fn [k] [k {:optional true}]) ks)
            schemas))

(defn- namespaced-ks [schemas ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys schemas)))

(defmethod clojure.schema/malli-form :s/components-ns [[_ ns-name-k] schemas]
  (clojure.schema/malli-form [:s/map-optional (namespaced-ks schemas ns-name-k)]
                         schemas))
