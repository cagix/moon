(ns forge.app.db
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.utils :refer [bind-root
                                 safe-get
                                 recur-sort-map
                                 apply-kvs
                                 async-pprint-spit!]]
            [malli.core :as m]
            [malli.error :as me]))

(defn schema-type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(declare schemas
         ^:private properties-file
         ^:private db-data)

(defn schema-of [k]
  (safe-get schemas k))

(defn- property-type [{:keys [property/id]}]
  (keyword "properties" (namespace id)))

(defn schema-of-property [property]
  (schema-of (property-type property)))

(defmulti malli-form schema-type)
(defmethod malli-form :default [schema] schema)

(defn- invalid-ex-info [m-schema value]
  (ex-info (str (me/humanize (m/explain m-schema value)))
           {:value value
            :schema (m/form m-schema)}))

#_(let [property {:entity/animation
                {:frame-duration 0.1,
                 :frames
                 ({:file "images/oryx_16bit_scifi_FX_lg_trans.png",
                   :sub-image-bounds [64 64 32 32]}
                  {:file "images/oryx_16bit_scifi_FX_lg_trans.png",
                   :sub-image-bounds [96 64 32 32]}),
                 :looping? false},
                :property/id :audiovisuals/convert,
                :tx/sound "bfxr_burrow"}]
  (malli-form (schema-of-property property))
  ; :s/map was undefined
  )

(defn- property->m-schema [property]
  (try (-> property
           schema-of-property
           malli-form
           m/schema)
       (catch clojure.lang.ExceptionInfo e
         (throw (ex-info "property->m-schema fail"
                         (merge (ex-data e)
                                {:property/id (:property/id property)
                                 :property property
                                 :schema-of-property (schema-of-property property)
                                 :malli-form (malli-form (schema-of-property property))}))))))

(defn validate! [property]
  (let [m-schema (property->m-schema property)]
    (when-not (m/validate m-schema property)
      (throw (invalid-ex-info m-schema property)))))

(defn create [[_ {:keys [schema properties]}]]
  (bind-root schemas (-> schema io/resource slurp edn/read-string))
  (bind-root properties-file (io/resource properties))
  (let [properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! validate! properties)
    (bind-root db-data (zipmap (map :property/id properties) properties))))

(defn- async-write-to-file! []
  (->> db-data
       vals
       (sort-by property-type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! properties-file)))

(defn update! [{:keys [property/id] :as property}]
  {:pre [(contains? property :property/id)
         (contains? db-data id)]}
  (validate! property)
  (alter-var-root #'db-data assoc id property)
  (async-write-to-file!))

(defn delete! [property-id]
  {:pre [(contains? db-data property-id)]}
  (alter-var-root #'db-data dissoc property-id)
  (async-write-to-file!))

(defn get-raw [id]
  (safe-get db-data id))

(defn all-raw [type]
  (->> (vals db-data)
       (filter #(= type (property-type %)))))

(declare build)

(defmulti edn->value (fn [schema v]
                       (when schema  ; undefined-data-ks
                         (schema-type schema))))
(defmethod edn->value :default [_schema v] v)

(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file
   :sub-image-bounds})

(defn- build* [property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema-of k)
                                 (catch Throwable _t
                                   (swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build* v)
                         v)]
                 (try (edn->value schema v)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))

(defn build [id]
  (build* (get-raw id)))

(defn build-all [type]
  (map build* (all-raw type)))

(defn property-types []
  (filter #(= "properties" (namespace %))
          (keys schemas)))

(defn migrate [property-type update-fn]
  (doseq [id (map :property/id (all-raw property-type))]
    (println id)
    (alter-var-root #'db-data update id update-fn))
  (async-write-to-file!))

