(ns cdq.ctx.create.ui.dev-menu
  (:require [cdq.db :as db]
            [cdq.graphics :as graphics]
            [cdq.input :as input]
            [cdq.ui :as ui]
            [cdq.ui.editor.overview-window :as editor-overview-window]
            [cdq.ui.editor.window :as editor-window]
            [clojure.string :as str]
            [clojure.vis-ui.label :as label]
            [cdq.world :as world]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.touchable :as touchable]
            [clojure.scene2d.vis-ui.menu :as menu]
            [cdq.ui.build.table :as table]
            [clojure.utils :as utils]))

(defn create [{:keys [ctx/db
                      ctx/graphics]}
              create-world]
  (let [open-editor (fn [db]
                      {:label "Editor"
                       :items (for [property-type (sort (db/property-types db))]
                                {:label (str/capitalize (name property-type))
                                 :on-click (fn [_actor {:keys [ctx/db
                                                               ctx/graphics
                                                               ctx/stage]}]
                                             (.addActor
                                              stage
                                              (editor-overview-window/create
                                               {:db db
                                                :graphics graphics
                                                :property-type property-type
                                                :clicked-id-fn (fn [_actor id {:keys [ctx/stage] :as ctx}]
                                                                 (.addActor stage
                                                                             (editor-window/create
                                                                              {:ctx ctx
                                                                               :property (db/get-raw db id)})))})))})})
        ctx-data-viewer {:label "Ctx Data"
                         :items [{:label "Show data"
                                  :on-click (fn [_actor {:keys [ctx/stage] :as ctx}]
                                              (ui/show-data-viewer! stage ctx))}]}
        help-info-text {:label "Help"
                        :items [{:label input/info-text}]}
        select-world {:label "Select World"
                      :items (for [world-fn ["world_fns/vampire.edn"
                                             "world_fns/uf_caves.edn"
                                             "world_fns/modules.edn"]]
                               {:label (str "Start " world-fn)
                                :on-click (fn [actor {:keys [ctx/stage] :as ctx}]
                                            (let [ui stage
                                                  stage (actor/stage actor)]  ; get before clear, otherwise the actor does not have a stage anymore
                                              (ui/rebuild-actors! ui ctx)
                                              (world/dispose! (:ctx/world ctx))
                                              (set! (.ctx stage) ((requiring-resolve create-world) ctx world-fn))))})}
        update-labels [{:label "elapsed-time"
                        :update-fn (fn [ctx]
                                     (str (utils/readable-number (:world/elapsed-time (:ctx/world ctx))) " seconds"))
                        :icon "images/clock.png"}
                       {:label "FPS"
                        :update-fn (fn [ctx]
                                     (graphics/frames-per-second (:ctx/graphics ctx)))
                        :icon "images/fps.png"}
                       {:label "Mouseover-entity id"
                        :update-fn (fn [{:keys [ctx/world]}]
                                     (let [eid (:world/mouseover-eid world)]
                                       (when-let [entity (and eid @eid)]
                                         (:entity/id entity))))
                        :icon "images/mouseover.png"}
                       {:label "paused?"
                        :update-fn (comp :world/paused? :ctx/world)}
                       {:label "GUI"
                        :update-fn (fn [{:keys [ctx/graphics]}]
                                     (mapv int (:graphics/ui-mouse-position graphics)))}
                       {:label "World"
                        :update-fn (fn [{:keys [ctx/graphics]}]
                                     (mapv int (:graphics/world-mouse-position graphics)))}
                       {:label "Zoom"
                        :update-fn (fn [ctx]
                                     (graphics/zoom (:ctx/graphics ctx)))
                        :icon "images/zoom.png"}]]
    (table/create
     {:rows [[{:actor (menu/create
                       {:menus [ctx-data-viewer
                                (open-editor db)
                                help-info-text
                                select-world]
                        :update-labels (for [item update-labels]
                                         (if (:icon item)
                                           (update item :icon #(get (:graphics/textures graphics) %))
                                           item))})
               :expand-x? true
               :fill-x? true
               :colspan 1}]
             [{:actor (doto (label/create "")
                        (actor/set-touchable! touchable/disabled))
               :expand? true
               :fill-x? true
               :fill-y? true}]]
      :fill-parent? true})))
