(ns cdq.create.schemas
  (:require cdq.assets
            [cdq.db :as db]
            cdq.graphics.animation
            cdq.graphics.sprite
            [cdq.schema :as schema]
            clojure.edn
            clojure.java.io))

(defn create [_context]
  (-> "schema.edn"
      clojure.java.io/resource
      slurp
      clojure.edn/read-string))

(defmethod schema/edn->value :s/sound [_ sound-name _db {:keys [cdq/assets]}]
  (cdq.assets/sound assets sound-name))

(defn- edn->sprite [c {:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (cdq.graphics.sprite/from-sheet (cdq.graphics.sprite/sheet c
                                                                         file
                                                                         tilew
                                                                         tileh)
                                          [(int (/ sprite-x tilew))
                                           (int (/ sprite-y tileh))]
                                          c))
    (cdq.graphics.sprite/create c file)))

(defmethod schema/edn->value :s/image [_ edn _db c]
  (edn->sprite c edn))

(defmethod schema/edn->value :s/animation [_ {:keys [frames frame-duration looping?]} _db c]
  (cdq.graphics.animation/create (map #(edn->sprite c %) frames)
                                     :frame-duration frame-duration
                                     :looping? looping?))

(defmethod schema/edn->value :s/one-to-one [_ property-id db {:keys [cdq/db] :as context}]
  (db/build db property-id context))

(defmethod schema/edn->value :s/one-to-many [_ property-ids db {:keys [cdq/db] :as context}]
  (set (map #(db/build db % context) property-ids)))
