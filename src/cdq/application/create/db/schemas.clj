(ns cdq.application.create.db.schemas
  (:require [cdq.db :as db]
            [cdq.graphics :as graphics]
            [cdq.schemas :as schemas]
            [cdq.string :as string]
            [cdq.property]
            [cdq.val-max :as val-max]
            cdq.schema.image
            cdq.schema.map
            cdq.schema.one-to-many
            cdq.schema.one-to-one
            cdq.schema.sound
            cdq.ui.editor.widget.default
            cdq.ui.editor.widget.edn
            [clojure.edn :as edn]))

(def schema-fn-map
  {
   :s/animation {'cdq.schema/malli-form   (fn [_ schemas]
                                            (schemas/create-map-schema schemas
                                                                       [:animation/frames
                                                                        :animation/frame-duration
                                                                        :animation/looping?]))
                 'cdq.schema/create-value (fn [_ v _db]
                                            v)
                 'cdq.schema/create       (fn [_ animation {:keys [ctx/graphics]}]
                                            {:actor/type :actor.type/table
                                             :rows [(for [image (:animation/frames animation)]
                                                      {:actor {:actor/type :actor.type/image-button
                                                               :drawable/texture-region (graphics/texture-region graphics image)
                                                               :drawable/scale 2}})]
                                             :cell-defaults {:pad 1}})
                 'cdq.schema/value        cdq.ui.editor.widget.default/value}

   :s/boolean {'cdq.schema/malli-form   (fn [[_ & params] _schemas]
                                          :boolean)
               'cdq.schema/create-value (fn [_ v _db]
                                          v)
               'cdq.schema/create       (fn [_ checked? _ctx]
                                          (assert (boolean? checked?))
                                          {:actor/type :actor.type/check-box
                                           :text ""
                                           :on-clicked (fn [_])
                                           :checked? checked?})
               'cdq.schema/value        (fn [_ widget _schemas]
                                          (:check-box/checked? widget))}

   :s/enum {'cdq.schema/malli-form   (fn [[_ & params] _schemas]
                                       (apply vector :enum params))
            'cdq.schema/create-value (fn [_ v _db]
                                       v)
            'cdq.schema/create       (fn [schema v _ctx]
                                       {:actor/type :actor.type/select-box
                                        :items (map string/->edn-str (rest schema))
                                        :selected (string/->edn-str v)})
            'cdq.schema/value        (fn [_  widget _schemas]
                                       (edn/read-string (:select-box/selected widget)))}

   :s/image {'cdq.schema/malli-form   (fn [_ schemas]
                                        (schemas/create-map-schema schemas
                                                                   [:image/file
                                                                    [:image/bounds {:optional true}]]))
             'cdq.schema/create-value (fn [_ v _db]
                                        v)
             'cdq.schema/create       cdq.schema.image/create
             'cdq.schema/value        cdq.ui.editor.widget.default/value}

   :s/map {'cdq.schema/malli-form   (fn [[_ ks] schemas]
                                      (schemas/create-map-schema schemas ks))
           'cdq.schema/create-value (fn [_ v db]
                                      (schemas/build-values (:db/schemas db) v db))
           'cdq.schema/create       cdq.schema.map/create
           'cdq.schema/value        cdq.schema.map/value}

   :s/number {'cdq.schema/malli-form   (fn [[_ predicate] _schemas]
                                         (case predicate
                                           :int     int?
                                           :nat-int nat-int?
                                           :any     number?
                                           :pos     pos?
                                           :pos-int pos-int?))
              'cdq.schema/create-value (fn [_ v _db]
                                         v)
              'cdq.schema/create       cdq.ui.editor.widget.edn/create
              'cdq.schema/value        cdq.ui.editor.widget.edn/value}

   :s/one-to-many {'cdq.schema/malli-form   (fn [[_ property-type] _schemas]
                                              [:set [:qualified-keyword {:namespace (cdq.property/type->id-namespace property-type)}]])
                   'cdq.schema/create-value (fn [_ property-ids db]
                                              (set (map (partial db/build db) property-ids)))
                   'cdq.schema/create       cdq.schema.one-to-many/create
                   'cdq.schema/value        cdq.schema.one-to-many/value}

   :s/one-to-one {'cdq.schema/malli-form   (fn [[_ property-type] _schemas]
                                             [:qualified-keyword {:namespace (cdq.property/type->id-namespace property-type)}])
                  'cdq.schema/create-value (fn [_ property-id db]
                                             (db/build db property-id))
                  'cdq.schema/create       cdq.schema.one-to-one/create
                  'cdq.schema/value        cdq.schema.one-to-one/value}

   :s/qualified-keyword {'cdq.schema/malli-form   (fn [[_ & params] _schemas]
                                                    (apply vector :qualified-keyword params))
                         'cdq.schema/create-value (fn [_ v _db]
                                                    v)
                         'cdq.schema/create       cdq.ui.editor.widget.default/create
                         'cdq.schema/value        cdq.ui.editor.widget.default/value}

   :s/some {'cdq.schema/malli-form   (fn [[_ & params] _schemas]
                                       :some)
            'cdq.schema/create-value (fn [_ v _db]
                                       v)
            'cdq.schema/create       cdq.ui.editor.widget.default/create
            'cdq.schema/value        cdq.ui.editor.widget.default/value}

   :s/sound {'cdq.schema/malli-form   (fn [_ _schemas]
                                        :string)
             'cdq.schema/create-value (fn [_ v _db]
                                        v)
             'cdq.schema/create       cdq.schema.sound/create
             'cdq.schema/value        cdq.ui.editor.widget.default/value}

   :s/string {'cdq.schema/malli-form   (fn [_ _schemas]
                                         :string)
              'cdq.schema/create-value (fn [_ v _db]
                                         v)
              'cdq.schema/create       (fn [schema  v _ctx]
                                         {:actor/type :actor.type/text-field
                                          :text-field/text v
                                          :tooltip (str schema)})
              'cdq.schema/value        (fn [_ widget _schemas]
                                         (:text-field/text widget))}

   :s/val-max {'cdq.schema/malli-form   (fn [_ _schemas]
                                          val-max/schema)
               'cdq.schema/create-value (fn [_ v _db]
                                          v)
               'cdq.schema/create       cdq.ui.editor.widget.edn/create
               'cdq.schema/value        cdq.ui.editor.widget.edn/value}

   :s/vector {'cdq.schema/malli-form   (fn [[_ & params] _schemas]
                                         (apply vector :vector params))
              'cdq.schema/create-value (fn [_ v _db]
                                         v)
              'cdq.schema/create       cdq.ui.editor.widget.default/create
              'cdq.schema/value        cdq.ui.editor.widget.default/value}
   }
  )
