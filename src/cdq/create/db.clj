(ns cdq.create.db
  (:require [cdq.db :as db]
            [cdq.schema :as schema]
            [cdq.schemas :as schemas]
            [cdq.property :as property]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [malli.core]
            [malli.map-schema :as map-schema]
            [malli.utils]
            cdq.schema.animation
            cdq.schema.boolean
            cdq.schema.enum
            cdq.schema.image
            cdq.schema.map
            cdq.schema.number
            cdq.schema.one-to-many
            cdq.schema.one-to-one
            cdq.schema.qualified-keyword
            cdq.schema.some
            cdq.schema.sound
            cdq.schema.string
            cdq.schema.val-max
            cdq.schema.vector
            cdq.ui.editor.widget.default
            cdq.ui.editor.widget.edn
            ))

(def ^:private schema-fn-map
  {
   :s/animation {'cdq.schema/malli-form   cdq.schema.animation/malli-form
                 'cdq.schema/create-value cdq.schema.animation/create-value
                 'cdq.schema/create       cdq.schema.animation/create
                 'cdq.schema/value        cdq.ui.editor.widget.default/value}

   :s/boolean {'cdq.schema/malli-form   cdq.schema.boolean/malli-form
               'cdq.schema/create-value cdq.schema.boolean/create-value
               'cdq.schema/create       cdq.schema.boolean/create
               'cdq.schema/value        cdq.schema.boolean/value}

   :s/enum {'cdq.schema/malli-form   cdq.schema.enum/malli-form
            'cdq.schema/create-value cdq.schema.enum/create-value
            'cdq.schema/create       cdq.schema.enum/create
            'cdq.schema/value        cdq.schema.enum/value}

   :s/image {'cdq.schema/malli-form   cdq.schema.image/malli-form
             'cdq.schema/create-value cdq.schema.image/create-value
             'cdq.schema/create       cdq.schema.image/create
             'cdq.schema/value        cdq.ui.editor.widget.default/value}

   :s/map {'cdq.schema/malli-form   cdq.schema.map/malli-form
           'cdq.schema/create-value cdq.schema.map/create-value
           'cdq.schema/create       cdq.schema.map/create
           'cdq.schema/value        cdq.schema.map/value}

   :s/number {'cdq.schema/malli-form   cdq.schema.number/malli-form
              'cdq.schema/create-value cdq.schema.number/create-value
              'cdq.schema/create       cdq.ui.editor.widget.edn/create
              'cdq.schema/value        cdq.ui.editor.widget.edn/value}

   :s/one-to-many {'cdq.schema/malli-form   cdq.schema.one-to-many/malli-form
                   'cdq.schema/create-value cdq.schema.one-to-many/create-value
                   'cdq.schema/create       cdq.schema.one-to-many/create
                   'cdq.schema/value        cdq.schema.one-to-many/value}

   :s/one-to-one {'cdq.schema/malli-form   cdq.schema.one-to-one/malli-form
                  'cdq.schema/create-value cdq.schema.one-to-one/create-value
                  'cdq.schema/create       cdq.schema.one-to-one/create
                  'cdq.schema/value        cdq.schema.one-to-one/value}

   :s/qualified-keyword {'cdq.schema/malli-form   cdq.schema.qualified-keyword/malli-form
                         'cdq.schema/create-value cdq.schema.qualified-keyword/create-value
                         'cdq.schema/create       cdq.ui.editor.widget.default/create
                         'cdq.schema/value        cdq.ui.editor.widget.default/value}

   :s/some {'cdq.schema/malli-form   cdq.schema.some/malli-form
            'cdq.schema/create-value cdq.schema.some/create-value
            'cdq.schema/create       cdq.ui.editor.widget.default/create
            'cdq.schema/value        cdq.ui.editor.widget.default/value}

   :s/sound {'cdq.schema/malli-form   cdq.schema.sound/malli-form
             'cdq.schema/create-value cdq.schema.sound/create-value
             'cdq.schema/create       cdq.schema.sound/create
             'cdq.schema/value        cdq.ui.editor.widget.default/value}

   :s/string {'cdq.schema/malli-form   cdq.schema.string/malli-form
              'cdq.schema/create-value cdq.schema.string/create-value
              'cdq.schema/create       cdq.schema.string/create
              'cdq.schema/value        cdq.schema.string/value}

   :s/val-max {'cdq.schema/malli-form   cdq.schema.val-max/malli-form
               'cdq.schema/create-value cdq.schema.val-max/create-value
               'cdq.schema/create       cdq.ui.editor.widget.edn/create
               'cdq.schema/value        cdq.ui.editor.widget.edn/value}

   :s/vector {'cdq.schema/malli-form   cdq.schema.vector/malli-form
              'cdq.schema/create-value cdq.schema.vector/create-value
              'cdq.schema/create       cdq.ui.editor.widget.default/create
              'cdq.schema/value        cdq.ui.editor.widget.default/value}
   }
  )

; reduce-kv?
(defn- apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

(defn- async-pprint-spit! [file data]
  (.start
   (Thread.
    (fn []
      (binding [*print-level* nil]
        (->> data
             pprint/pprint
             with-out-str
             (spit file)))))))

(defn- recur-sort-map [m]
  (into (sorted-map)
        (zipmap (keys m)
                (map #(if (map? %)
                        (recur-sort-map %)
                        %)
                     (vals m)))))

(defn save-vals! [data-vals file]
  (->> data-vals
       (sort-by property/type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! file)))

(defn- save! [{:keys [db/data db/file]}]
  ; TODO validate them again!?
  (save-vals! (vals data)
              file))

(defn- validate-property [schemas property]
  (schemas/validate schemas (property/type property) property))

(defn- validate-properties! [schemas properties]
  (assert (or (empty? properties)
              (apply distinct? (map :property/id properties))))
  (doseq [property properties]
    (validate-property schemas property)))

(defrecord Schemas []
  schemas/Schemas
  (build-values [schemas property db]
    (apply-kvs property
               (fn [k v]
                 (try (schema/create-value (get schemas k) v db)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t)))))))

  (default-value [schemas k]
    (let [schema (get schemas k)]
      (cond
       ;(#{:s/one-to-one :s/one-to-many} (get-type schema)) nil
       (#{:s/map} (schema 0)) {} ; cannot have empty for required keys, then no Add Component button
       :else nil
       ;:else (m/generate (schema/malli-form schema schemas) {:size 3})

       )))

  (validate [schemas k value]
    (-> (get schemas k)
        (schema/malli-form schemas)
        malli.core/schema
        (malli.utils/validate-humanize value)))

  (create-map-schema [schemas ks]
    (map-schema/create-map-schema ks (fn [k]
                                       (schema/malli-form (get schemas k) schemas)))))

(defrecord DB []
  db/DB
  (property-types [{:keys [db/schemas]}]
    (filter #(= "properties" (namespace %)) (keys schemas)))

  (update! [{:keys [db/data db/schemas]
             :as this}
            {:keys [property/id] :as property}]
    (assert (contains? property :property/id))
    (assert (contains? data id))
    (validate-property schemas property)
    (let [new-db (update this :db/data assoc id property)]
      (save! new-db)
      new-db))

  (delete! [{:keys [db/data] :as this} property-id]
    (assert (contains? data property-id))
    (let [new-db (update this :db/data dissoc property-id)]
      (save! new-db)
      new-db))

  (get-raw [{:keys [db/data]} property-id]
    {:pre [(contains? data property-id)]}
    (get data property-id))

  (all-raw [{:keys [db/data]} property-type]
    (->> (vals data)
         (filter #(= property-type (property/type %)))))

  (build
    [{:keys [db/schemas]
      :as this}
     property-id]
    (schemas/build-values schemas
                          (db/get-raw this property-id)
                          this))

  (build-all
    [{:keys [db/schemas]
      :as this}
     property-type]
    (map #(schemas/build-values schemas % this)
         (db/all-raw this property-type))))

(defn create
  []
  (let [schemas "schema.edn"
        properties "properties.edn"
        schemas (update-vals (-> schemas io/resource slurp edn/read-string)
                             (fn [[k :as schema]]
                               (with-meta schema (get schema-fn-map k))))
        schemas (map->Schemas schemas)
        properties-file (io/resource properties)
        properties (-> properties-file slurp edn/read-string)]
    (validate-properties! schemas properties)
    (merge (map->DB {})
           {:db/data (zipmap (map :property/id properties) properties)
            :db/file properties-file
            :db/schemas schemas})))
