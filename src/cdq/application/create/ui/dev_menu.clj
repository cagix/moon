(ns cdq.application.create.ui.dev-menu
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.ui.widget :as widget]
            [cdq.ui.editor.window]
            [clojure.string :as str]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.stage :as stage]))

(def ^:private world-fns
  ["world_fns/vampire.edn"
   "world_fns/uf_caves.edn"
   "world_fns/modules.edn"])

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
                         (ctx/open-editor-overview! ctx
                                                    {:property-type property-type
                                                     :clicked-id-fn (fn [id {:keys [ctx/db] :as ctx}]
                                                                      (cdq.ui.editor.window/add-to-stage! ctx
                                                                                                          (db/get-raw db id)))}))})})

(def ^:private select-world
  {:label "Select World"
   :items (for [world-fn world-fns]
            {:label (str "Start " world-fn)
             :on-click (fn [actor ctx]
                         (stage/set-ctx! (actor/get-stage actor)
                                         (ctx/reset-game-state! ctx world-fn)))})})

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
