(ns cdq.editor.widgets
  (:require cdq.editor-window
            [cdq.audio :as audio]
            [cdq.db :as db]
            [cdq.image :as image]
            [cdq.property :as property]
            [cdq.schemas :as schemas]
            [cdq.editor.overview-table]
            [cdq.editor.widget :as editor-widget]
            [cdq.ui.widget]
            [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.gdx.scenes.scene2d.ui.window :as window]
            [clojure.set :as set]
            [clojure.vis-ui.check-box :as check-box]
            [clojure.vis-ui.select-box :as select-box]
            [clojure.vis-ui.separator :as separator]
            [clojure.vis-ui.text-field :as text-field]
            [clojure.vis-ui.tooltip :as tooltip]
            [clojure.vis-ui.widget :as widget]))

(defn- play-button [sound-name]
  (widget/text-button "play!"
                      (fn [_actor {:keys [ctx/audio]}]
                        (audio/play-sound! audio sound-name))))

(declare sound-columns)

(defn- open-choose-sound-window! [table
                                  {:keys [ctx/audio
                                          ctx/stage
                                          ctx/ui-viewport]}]
  (let [rows (for [sound-name (audio/all-sounds audio)]
               [(widget/text-button sound-name
                                    (fn [actor _ctx]
                                      (group/clear-children! table)
                                      (table/add-rows! table [(sound-columns table sound-name)])
                                      (.remove (window/find-ancestor actor))
                                      (window/pack-ancestors! table)
                                      (let [[k _] (actor/user-object table)]
                                        (actor/set-user-object! table [k sound-name]))))
                (play-button sound-name)])]
    (stage/add! stage (cdq.ui.widget/scroll-pane-window (:viewport/width ui-viewport)
                                                        rows))))

(defn- sound-columns [table sound-name]
  [(widget/text-button sound-name
                       (fn [_actor ctx]
                         (open-choose-sound-window! table ctx)))
   (play-button sound-name)])


(defn- add-one-to-one-rows
  [{:keys [ctx/db
           ctx/textures]}
   table
   property-type
   property-id]
  (let [redo-rows (fn [ctx id]
                    (group/clear-children! table)
                    (add-one-to-one-rows ctx table property-type id)
                    (window/pack-ancestors! table))]
    (table/add-rows!
     table
     [[(when-not property-id
         (widget/text-button "+"
                             (fn [_actor {:keys [ctx/stage] :as ctx}]
                               (let [window (widget/window {:title "Choose"
                                                            :modal? true
                                                            :close-button? true
                                                            :center? true
                                                            :close-on-escape? true})
                                     clicked-id-fn (fn [id ctx]
                                                     (.remove window)
                                                     (redo-rows ctx id))]
                                 (table/add! window (cdq.editor.overview-table/create ctx property-type clicked-id-fn))
                                 (.pack window)
                                 (stage/add! stage window)))))]
      [(when property-id
         (let [property (db/get-raw db property-id)
               texture-region (image/texture-region (property/image property) textures)
               image-widget (widget/image texture-region
                                          {:id property-id})]
           (tooltip/add! image-widget (utils/pprint-to-str property))
           image-widget))]
      [(when property-id
         (widget/text-button "-"
                             (fn [_actor ctx]
                               (redo-rows ctx nil))))]])))

(defn- add-one-to-many-rows
  [{:keys [ctx/db
           ctx/textures]}
   table
   property-type
   property-ids]
  (let [redo-rows (fn [ctx property-ids]
                    (group/clear-children! table)
                    (add-one-to-many-rows ctx table property-type property-ids)
                    (window/pack-ancestors! table))]
    (table/add-rows!
     table
     [[(widget/text-button "+"
                           (fn [_actor {:keys [ctx/stage] :as ctx}]
                             (let [window (widget/window {:title "Choose"
                                                          :modal? true
                                                          :close-button? true
                                                          :center? true
                                                          :close-on-escape? true})
                                   clicked-id-fn (fn [id ctx]
                                                   (.remove window)
                                                   (redo-rows ctx (conj property-ids id)))]
                               (table/add! window (cdq.editor.overview-table/create ctx property-type clicked-id-fn))
                               (.pack window)
                               (stage/add! stage window))))]
      (for [property-id property-ids]
        (let [property (db/get-raw db property-id)
              texture-region (image/texture-region (property/image property) textures)
              image-widget (widget/image texture-region {:id property-id})]
          (tooltip/add! image-widget (utils/pprint-to-str property))))
      (for [id property-ids]
        (widget/text-button "-"
                            (fn [_actor ctx]
                              (redo-rows ctx (disj property-ids id)))))])))

(def ^:private property-k-sort-order
  [:property/id
   :property/pretty-name
   :entity/image
   :entity/animation
   :entity/species
   :creature/level
   :entity/body
   :item/slot
   :projectile/speed
   :projectile/max-range
   :projectile/piercing?
   :skill/action-time-modifier-key
   :skill/action-time
   :skill/start-action-sound
   :skill/cost
   :skill/cooldown])

(defn- window->property-value [property-editor-window schemas]
 (let [window property-editor-window
       scroll-pane-table (group/find-actor (:scroll-pane window) "scroll-pane-table")
       m-widget-cell (first (seq (table/cells scroll-pane-table)))
       table (:map-widget scroll-pane-table)]
   (editor-widget/value [:s/map] nil table schemas)))

(defn- rebuild-editor-window!
  [{:keys [ctx/db
           ctx/stage]
    :as ctx}]
  (let [window (:property-editor-window stage)
        prop-value (window->property-value window (:schemas db))]
    (actor/remove! window)
    (stage/add! stage (cdq.editor-window/property-editor-window ctx prop-value))))

(defn- find-kv-widget [table k]
  (utils/find-first (fn [actor]
                      (and (actor/user-object actor)
                           (= k ((actor/user-object actor) 0))))
                    (group/children table)))

(def ^:private component-row-cols 3)

(defn- component-row [ctx [k v] map-schema schemas table]
  [{:actor {:actor/type :actor.type/table
            :cell-defaults {:pad 2}
            :rows [[{:actor (when (schemas/optional-k? schemas map-schema k)
                              (widget/text-button "-"
                                                  (fn [_actor ctx]
                                                    (actor/remove! (find-kv-widget table k))
                                                    (rebuild-editor-window! ctx))))
                     :left? true}
                    {:actor {:actor/type :actor.type/label
                             :label/text (name k) ;(str "[GRAY]:" (namespace k) "[]/" (name k))
                             }}]]}
    :right? true}
   {:actor (separator/vertical)
    :pad-top 2
    :pad-bottom 2
    :fill-y? true
    :expand-y? true}
   {:actor (let [widget (actor/build? (editor-widget/create (get schemas k) k v ctx))]
             (actor/set-user-object! widget [k v])
             widget)
    :left? true}])

(defn- open-add-component-window! [{:keys [ctx/db
                                           ctx/stage]}
                                   schema
                                   map-widget-table]
  (let [schemas (:schemas db)
        window (widget/window {:title "Choose"
                               :modal? true
                               :close-button? true
                               :center? true
                               :close-on-escape? true
                               :cell-defaults {:pad 5}})
        remaining-ks (sort (remove (set (keys (editor-widget/value schema nil map-widget-table schemas)))
                                   (schemas/map-keys schemas schema)))]
    (table/add-rows!
     window
     (for [k remaining-ks]
       [(widget/text-button (name k)
                            (fn [_actor ctx]
                              (.remove window)
                              (table/add-rows! map-widget-table [(component-row ctx
                                                                                [k (schemas/k->default-value schemas k)]
                                                                                schema
                                                                                schemas
                                                                                map-widget-table)])
                              (rebuild-editor-window! ctx)))]))
    (.pack window)
    (stage/add! stage window)))

(defn- horiz-sep []
  [{:actor (separator/horizontal)
    :pad-top 2
    :pad-bottom 2
    :colspan component-row-cols
    :fill-x? true
    :expand-x? true}])

(defn- interpose-f [f coll]
  (drop 1 (interleave (repeatedly f) coll)))

(defn do! [ctx]
  ctx)

(def k->methods
  {:s/map {:create (fn [schema  _attribute m {:keys [ctx/db] :as ctx}]
                     (let [table (widget/table
                                  {:cell-defaults {:pad 5}
                                   :id :map-widget})
                           component-rows (interpose-f horiz-sep
                                                       (map (fn [[k v]]
                                                              (component-row ctx
                                                                             [k v]
                                                                             schema
                                                                             (:schemas db)
                                                                             table))
                                                            (utils/sort-by-k-order property-k-sort-order
                                                                                   m)))
                           colspan component-row-cols
                           opt? (seq (set/difference (schemas/optional-keyset (:schemas db) schema)
                                                     (set (keys m))))]
                       (table/add-rows!
                        table
                        (concat [(when opt?
                                   [{:actor (widget/text-button "Add component"
                                                                (fn [_actor ctx]
                                                                  (open-add-component-window! ctx schema table)))
                                     :colspan colspan}])]
                                [(when opt?
                                   [{:actor (separator/horizontal)
                                     :pad-top 2
                                     :pad-bottom 2
                                     :colspan colspan
                                     :fill-x? true
                                     :expand-x? true}])]
                                component-rows))
                       table))
           :value (fn [_  _attribute table schemas]
                    (into {}
                          (for [widget (filter (comp vector? actor/user-object) (group/children table))
                                :let [[k _] (actor/user-object widget)]]
                            [k (editor-widget/value (get schemas k) k widget schemas)])))}


   :default {:create (fn [_ _attribute v _ctx]
                       {:actor/type :actor.type/label
                        :label/text (utils/truncate (utils/->edn-str v) 60)})
             :value (fn [_  _attribute widget _schemas]
                      ((actor/user-object widget) 1))
             }

   :widget/edn {:create (fn [schema  _attribute v _ctx]
                          {:actor/type :actor.type/text-field
                           :text-field/text (utils/->edn-str v)
                           :tooltip (str schema)})
                :value (fn [_  _attribute widget _schemas]
                         (edn/read-string (text-field/get-text widget)))}

   :string {:create (fn [schema  _attribute v _ctx]
                      {:actor/type :actor.type/text-field
                       :text-field/text v
                       :tooltip (str schema)})

            :value (fn [_  _attribute widget _schemas]
                     (text-field/get-text widget))}

   :boolean {:create (fn [_ _attribute checked? _ctx]
                       (assert (boolean? checked?))
                       {:actor/type :actor.type/check-box
                        :text ""
                        :on-clicked (fn [_])
                        :checked? checked?})

             :value (fn [_ _attribute widget _schemas]
                      (check-box/checked? widget))}

   :enum {
          :create (fn [schema _attribute v _ctx]
                    {:actor/type :actor.type/select-box
                     :items (map utils/->edn-str (rest schema))
                     :selected (utils/->edn-str v)})

          :value (fn [_  _attribute widget _schemas]
                   (edn/read-string (select-box/get-selected widget)))

          }

   :s/sound {

             :create (fn [_  _attribute sound-name _ctx]
                       (let [table (widget/table {:cell-defaults {:pad 5}})]
                         (table/add-rows! table [(if sound-name
                                                   (sound-columns table sound-name)
                                                   [(widget/text-button "No sound"
                                                                        (fn [_actor ctx]
                                                                          (open-choose-sound-window! table ctx)))])])
                         table))
             }

   :s/one-to-one {

                  :create (fn [[_ property-type]  _attribute property-id ctx]
                            (let [table (widget/table {:cell-defaults {:pad 5}})]
                              (add-one-to-one-rows ctx table property-type property-id)
                              table))

                  :value (fn [_  _attribute widget _schemas]
                           (->> (group/children widget)
                                (keep actor/user-object)
                                first))
                  }

   :s/one-to-many {

                   :create (fn [[_ property-type]  _attribute property-ids ctx]
                             (let [table (widget/table {:cell-defaults {:pad 5}})]
                               (add-one-to-many-rows ctx table property-type property-ids)
                               table))

                   :value (fn [_  _attribute widget _schemas]
                            (->> (group/children widget)
                                 (keep actor/user-object)
                                 set))
                   }

   :widget/image {
                  ; too many ! too big ! scroll ... only show files first & preview?
                  ; make tree view from folders, etc. .. !! all creatures animations showing...
                  #_(defn- texture-rows [ctx]
                      (for [file (sort (assets/all-of-type assets :texture))]
                        [(image-button/create {:texture-region (texture/region (assets file))})]
                        #_[(text-button/create file
                                               (fn [_actor _ctx]))]))

                  :create (fn [schema  _attribute image {:keys [ctx/textures]}]
                            {:actor/type :actor.type/image-button
                             :drawable/texture-region (image/texture-region image textures)
                             :drawable/scale 2}
                            #_(ui/image-button image
                                               (fn [_actor ctx]
                                                 (c/add-actor! ctx (scroll-pane/choose-window (texture-rows ctx))))
                                               {:dimensions [96 96]})) ; x2  , not hardcoded here
                  }

   :widget/animation {

                      :create (fn [_ _attribute animation {:keys [ctx/textures]}]
                                {:actor/type :actor.type/table
                                 :rows [(for [image (:animation/frames animation)]
                                          {:actor {:actor/type :actor.type/image-button
                                                   :drawable/texture-region (image/texture-region image textures)
                                                   :drawable/scale 2}})]
                                 :cell-defaults {:pad 1}})
                      }

   })
