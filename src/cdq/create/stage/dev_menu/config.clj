(ns cdq.create.stage.dev-menu.config
  (:require cdq.application
            [cdq.db :as db]
            cdq.editor
            cdq.graphics
            [cdq.graphics.camera :as cam]
            [cdq.stage :as stage]
            [cdq.ui :as ui]
            cdq.world.context
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.gdx.scenes.scene2d.ui.widget-group :as widget-group]
            [clojure.string :as str]
            [clojure.utils :refer [readable-number]]))

;"Mouseover-Actor: "
#_(when-let [actor (stage/mouse-on-actor? context)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn create [{:keys [cdq/db] :as c}]
  {:menus [{:label "World"
            :items (for [world (map (fn [id] (db/build db id c))
                                    [:worlds/vampire
                                     :worlds/modules
                                     :worlds/uf-caves])]
                     {:label (str "Start " (:property/id world))
                      :on-click (fn [_context]
                                  (swap! cdq.application/state cdq.world.context/reset (:property/id world)))})}
           ; TODO fixme does not work because create world uses create-into which checks key is not preseent
           ; => look at cleanup-world/reset-state/ (camera not reset - mutable state be careful ! -> create new cameras?!)
           ; => also world-change should be supported, use component systems
           {:label "Help"
            :items [{:label "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}]}
           {:label "Objects"
            :items (for [property-type (sort (filter #(= "properties" (namespace %))
                                                     (keys (:cdq/schemas c))))]
                     {:label (str/capitalize (name property-type))
                      :on-click (fn [context]
                                  (let [window (ui/window {:title "Edit"
                                                           :modal? true
                                                           :close-button? true
                                                           :center? true
                                                           :close-on-escape? true})]
                                    (table/add! window (cdq.editor/overview-table context
                                                                                  property-type
                                                                                  cdq.editor/edit-property))
                                    (widget-group/pack! window)
                                    (stage/add-actor (:cdq.context/stage context)
                                                     window)))})}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn [{:keys [cdq.context/mouseover-eid]}]
                                 (when-let [entity (and mouseover-eid @mouseover-eid)]
                                   (:entity/id entity)))
                    :icon "images/mouseover.png"}
                   {:label "elapsed-time"
                    :update-fn (fn [{:keys [cdq.context/elapsed-time]}]
                                 (str (readable-number elapsed-time) " seconds"))
                    :icon "images/clock.png"}
                   {:label "paused?"
                    :update-fn :cdq.context/paused?} ; TODO (def paused ::paused) @ cdq.context
                   {:label "GUI"
                    :update-fn (comp cdq.graphics/mouse-position
                                     :cdq.graphics/ui-viewport)}
                   {:label "World"
                    :update-fn #(mapv int (cdq.graphics/world-mouse-position (:cdq.graphics/world-viewport %)))}
                   {:label "Zoom"
                    :update-fn #(cam/zoom (:camera (:cdq.graphics/world-viewport %)))
                    :icon "images/zoom.png"}
                   {:label "FPS"
                    :update-fn (fn [_]
                                 (graphics/frames-per-second))
                    :icon "images/fps.png"}]})
