(ns cdq.create.ui.dev-menu
  (:require [cdq.create.reset-world]
            [cdq.create.spawn-player]
            [cdq.create.spawn-enemies]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.ui.widget :as widget]
            [cdq.world-fns.tmx]
            [cdq.world-fns.uf-caves]
            [cdq.world-fns.modules]
            [clojure.string :as str]
            [clojure.disposable :as disposable]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.stage :as stage]))

(def ^:private world-fns
  [[cdq.world-fns.tmx/create
    {:tmx-file "maps/vampire.tmx"
     :start-position [32 71]}]
   [cdq.world-fns.uf-caves/create
    {:tile-size 48
     :texture-path "maps/uf_terrain.png"
     :spawn-rate 0.02
     :scaling 3
     :cave-size 200
     :cave-style :wide}]
   [cdq.world-fns.modules/create
    {:world/map-size 5,
     :world/max-area-level 3,
     :world/spawn-rate 0.05}]])

(def ^:private help-str
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")

(def ^:private help-info-text
  {:label "Help"
   :items [{:label help-str}]})

(def ^:private ctx-data-viewer
  {:label "Ctx Data"
   :items [{:label "Show data"
            :on-click (fn [_actor {:keys [ctx/stage] :as ctx}]
                        (stage/add! stage (widget/data-viewer
                                           {:title "Context"
                                            :data ctx
                                            :width 500
                                            :height 500})))}]})

(defn- open-editor [db]
  {:label "Editor"
   :items (for [property-type (sort (db/property-types db))]
            {:label (str/capitalize (name property-type))
             :on-click (fn [_actor ctx]
                         (ctx/handle-txs! ctx
                                          [[:tx/open-editor-overview
                                            {:property-type property-type
                                             :clicked-id-fn (fn [id {:keys [ctx/db] :as ctx}]
                                                              (ctx/handle-txs! ctx [[:tx/open-property-editor (db/get-raw db id)]]))}]]))})})

(def ^:private select-world
  {:label "Select World"
   :items (for [world-fn world-fns]
            {:label (str "Start " (first world-fn))
             :on-click (fn [actor {:keys [ctx/world]
                                    :as ctx}]
                         (disposable/dispose! world)
                         (let [stage (actor/get-stage actor) ; before reset-stage!!!!
                               _ ((requiring-resolve 'cdq.create.reset-stage/do!) ctx)
                               new-ctx (-> ctx
                                           (cdq.create.reset-world/do! world-fn)
                                           cdq.create.spawn-player/do!
                                           cdq.create.spawn-enemies/do!)]
                           (stage/set-ctx! stage new-ctx)))})})

(defn create
  [{:keys [ctx/db
           ctx/graphics]}
   {:keys [update-labels]}]
  {:actor/type :actor.type/table
   :rows [[{:actor {:actor/type :actor.type/menu-bar
                    :menus [ctx-data-viewer
                            (open-editor db)
                            help-info-text
                            select-world]
                    :update-labels (for [item update-labels]
                                     (if (:icon item)
                                       (update item :icon #(get (:graphics/textures graphics) %))
                                       item))}
            :expand-x? true
            :fill-x? true
            :colspan 1}]
          [{:actor {:actor/type :actor.type/label
                    :label/text ""
                    :actor/touchable :disabled}
            :expand? true
            :fill-x? true
            :fill-y? true}]]
   :fill-parent? true})
