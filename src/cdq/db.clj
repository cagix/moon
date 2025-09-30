(ns cdq.db
  (:require [cdq.db.property :as property]
            [cdq.db.schema :as schema]
            [cdq.db.schemas :as schemas]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [cdq.malli :as m]
            [gdl.utils :as utils]))

(defprotocol PDB
  (property-types [_])
  (update!  [_ property])
  (delete!  [_ property-id])
  (get-raw [_ property-id])
  (all-raw [_ property-type])
  (build [_ property-id])
  (build-all [_ property-type]))


(defn- save!
  [{:keys [db/data db/file]}]
  (let [data (->> (vals data)
                  (sort-by property/type)
                  (map utils/recur-sort-map)
                  doall)]
    (.start
     (Thread.
      (fn []
        (binding [*print-level* nil]
          (->> data
               pprint/pprint
               with-out-str
               (spit file))))))))

(defrecord Schemas []
  schemas/Schemas
  (build-values [schemas property db]
    (utils/apply-kvs property
                     (fn [k v]
                       (try (schema/create-value (get schemas k) v db)
                            (catch Throwable t
                              (throw (ex-info " " {:k k :v v} t)))))))

  (default-value [schemas k]
    (let [schema (get schemas k)]
      (cond
       (#{:s/map} (schema 0)) {}
       :else nil)))

  (validate [schemas k value]
    (-> (get schemas k)
        (schema/malli-form schemas)
        m/schema
        (m/validate-humanize value)))

  (create-map-schema [schemas ks]
    (m/create-map-schema ks (fn [k]
                              (schema/malli-form (get schemas k) schemas)))))

(defrecord RDB []
  PDB
  (property-types [{:keys [db/schemas]}]
    (filter #(= "properties" (namespace %)) (keys schemas)))

  (update! [{:keys [db/data db/schemas]
             :as this}
            {:keys [property/id] :as property}]
    (assert (contains? property :property/id))
    (assert (contains? data id))
    (schemas/validate schemas (property/type property) property)
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
                          (get-raw this property-id)
                          this))

  (build-all
    [{:keys [db/schemas]
      :as this}
     property-type]
    (map #(schemas/build-values schemas % this)
         (all-raw this property-type))))

(defn- create*
  [{:keys [schemas
           properties
           schema-fn-map]}]
  (let [schemas (update-vals (-> schemas io/resource slurp edn/read-string)
                             (fn [[k :as schema]]
                               (with-meta schema (get schema-fn-map k))))
        schemas (map->Schemas schemas)
        properties-file (io/resource properties)
        properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (doseq [property properties]
      (schemas/validate schemas (property/type property) property))
    (merge (map->RDB {})
           {:db/data (zipmap (map :property/id properties) properties)
            :db/file properties-file
            :db/schemas schemas})))

(def ^:private schema-fn-map
  '{
    :s/animation {cdq.db.schema/malli-form   cdq.db.schema.animation/malli-form
                  cdq.db.schema/create-value cdq.db.schema.animation/create-value
                  cdq.db.schema/create       cdq.db.schema.animation/create
                  cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/boolean {cdq.db.schema/malli-form   cdq.db.schema.boolean/malli-form
                cdq.db.schema/create-value cdq.db.schema.boolean/create-value
                cdq.db.schema/create       cdq.db.schema.boolean/create
                cdq.db.schema/value        cdq.db.schema.boolean/value}

    :s/enum {cdq.db.schema/malli-form   cdq.db.schema.enum/malli-form
             cdq.db.schema/create-value cdq.db.schema.enum/create-value
             cdq.db.schema/create       cdq.db.schema.enum/create
             cdq.db.schema/value        cdq.db.schema.enum/value}

    :s/image {cdq.db.schema/malli-form   cdq.db.schema.image/malli-form
              cdq.db.schema/create-value cdq.db.schema.image/create-value
              cdq.db.schema/create       cdq.db.schema.image/create
              cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/map {cdq.db.schema/malli-form   cdq.db.schema.map/malli-form
            cdq.db.schema/create-value cdq.db.schema.map/create-value
            cdq.db.schema/create       cdq.db.schema.map/create
            cdq.db.schema/value        cdq.db.schema.map/value}

    :s/number {cdq.db.schema/malli-form   cdq.db.schema.number/malli-form
               cdq.db.schema/create-value cdq.db.schema.number/create-value
               cdq.db.schema/create       cdq.ui.editor.widget.edn/create
               cdq.db.schema/value        cdq.ui.editor.widget.edn/value}

    :s/one-to-many {cdq.db.schema/malli-form   cdq.db.schema.one-to-many/malli-form
                    cdq.db.schema/create-value cdq.db.schema.one-to-many/create-value
                    cdq.db.schema/create       cdq.db.schema.one-to-many/create
                    cdq.db.schema/value        cdq.db.schema.one-to-many/value}

    :s/one-to-one {cdq.db.schema/malli-form   cdq.db.schema.one-to-one/malli-form
                   cdq.db.schema/create-value cdq.db.schema.one-to-one/create-value
                   cdq.db.schema/create       cdq.db.schema.one-to-one/create
                   cdq.db.schema/value        cdq.db.schema.one-to-one/value}

    :s/qualified-keyword {cdq.db.schema/malli-form   cdq.db.schema.qualified-keyword/malli-form
                          cdq.db.schema/create-value cdq.db.schema.qualified-keyword/create-value
                          cdq.db.schema/create       cdq.ui.editor.widget.default/create
                          cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/some {cdq.db.schema/malli-form   cdq.db.schema.some/malli-form
             cdq.db.schema/create-value cdq.db.schema.some/create-value
             cdq.db.schema/create       cdq.ui.editor.widget.default/create
             cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/sound {cdq.db.schema/malli-form   cdq.db.schema.sound/malli-form
              cdq.db.schema/create-value cdq.db.schema.sound/create-value
              cdq.db.schema/create       cdq.db.schema.sound/create
              cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/string {cdq.db.schema/malli-form   cdq.db.schema.string/malli-form
               cdq.db.schema/create-value cdq.db.schema.string/create-value
               cdq.db.schema/create       cdq.db.schema.string/create
               cdq.db.schema/value        cdq.db.schema.string/value}

    :s/val-max {cdq.db.schema/malli-form   cdq.db.schema.val-max/malli-form
                cdq.db.schema/create-value cdq.db.schema.val-max/create-value
                cdq.db.schema/create       cdq.ui.editor.widget.edn/create
                cdq.db.schema/value        cdq.ui.editor.widget.edn/value}

    :s/vector {cdq.db.schema/malli-form   cdq.db.schema.vector/malli-form
               cdq.db.schema/create-value cdq.db.schema.vector/create-value
               cdq.db.schema/create       cdq.ui.editor.widget.default/create
               cdq.db.schema/value        cdq.ui.editor.widget.default/value}
    }
  )

(alter-var-root #'schema-fn-map update-vals (fn [method-map]
                                              (update-vals method-map
                                                           (fn [sym]
                                                             (let [avar (requiring-resolve sym)]
                                                               (assert avar sym)
                                                               avar)))))

(defn create []
  (create* {:schemas "schema.edn"
            :properties "properties.edn"
            :schema-fn-map schema-fn-map}))
