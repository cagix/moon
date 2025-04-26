(ns cdq.impl.ui.dev-menu
  (:require cdq.application
            [cdq.db :as db]
            cdq.editor
            [cdq.graphics :as graphics]
            [cdq.graphics.camera :as cam]
            cdq.graphics.sprite
            [cdq.ui :as ui :refer [ui-actor]]
            [cdq.ui.group :refer [add-actor!]]
            [cdq.ui.stage :as stage]
            [cdq.ui.table :as table]
            [cdq.utils :refer [readable-number]]
            cdq.world.context
            [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.scenes.scene2d Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Label Table)
           (com.kotcrab.vis.ui.widget PopupMenu)))

;"Mouseover-Actor: "
#_(when-let [actor (stage/mouse-on-actor? context)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn- create-config [{:keys [cdq/db] :as c}]
  {:menus [{:label "World"
            :items (for [world (map (fn [id] (db/build db id c))
                                    [:worlds/vampire
                                     :worlds/modules
                                     :worlds/uf-caves])]
                     {:label (str "Start " (:property/id world))
                      :on-click (fn [_context]
                                  ; FIXME SEVERE
                                  ; passing outdated context!
                                  ; do not use cdq.application/state in ui contexts -> grep!
                                  ; (stage .act is called via passing context in the main 'swap!' of the application loop)
                                  ; (swap! state render)
                                  ; cdq.render.stage pass .applicationState and return
                                  (swap! cdq.application/state cdq.world.context/reset {:world-id (:property/id world)}))})}
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
                                    (ui/pack! window)
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
                    :update-fn (comp graphics/mouse-position
                                     :cdq.graphics/ui-viewport)}
                   {:label "World"
                    :update-fn #(mapv int (graphics/world-mouse-position (:cdq.graphics/world-viewport %)))}
                   {:label "Zoom"
                    :update-fn #(cam/zoom (:camera (:cdq.graphics/world-viewport %)))
                    :icon "images/zoom.png"}
                   {:label "FPS"
                    :update-fn (fn [_]
                                 (.getFramesPerSecond Gdx/graphics))
                    :icon "images/fps.png"}]})

(defn- menu-item [text on-clicked]
  (doto (ui/menu-item text)
    (.addListener (ui/change-listener on-clicked))))

(defn- set-label-text-fn [label text-fn]
  (fn [context]
    (Label/.setText label (str (text-fn context)))))

(defn- add-upd-label
  ([c table text-fn icon]
   (let [icon (ui/image->widget (cdq.graphics.sprite/create c icon) {})
         label (ui/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (add-actor! table (ui-actor {:act (set-label-text-fn label text-fn)}))
     (.expandX (.right (Table/.add table sub-table)))))
  ([c table text-fn]
   (let [label (ui/label "")]
     (add-actor! table (ui-actor {:act (set-label-text-fn label text-fn)}))
     (.expandX (.right (Table/.add table label))))))

(defn- add-update-labels [c menu-bar update-labels]
  (let [table (ui/menu-bar->table menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn %))]
        (if icon
          (add-upd-label c table update-fn icon)
          (add-upd-label c table update-fn))))))

(defn- add-menu [c menu-bar {:keys [label items]}]
  (let [app-menu (ui/menu label)]
    (doseq [{:keys [label on-click]} items]
      (PopupMenu/.addItem app-menu (menu-item label (if on-click
                                                      #(on-click @cdq.application/state) ;=> change-listener get .application-state @ ui but not sure if it has that or go through actor
                                                      (fn [])))))
    (ui/add-menu menu-bar app-menu)))

(defn- create-menu-bar [c menus]
  (let [menu-bar (ui/menu-bar)]
    (run! #(add-menu c menu-bar %) menus)
    menu-bar))

(defn- dev-menu* [c {:keys [menus update-labels]}]
  (let [menu-bar (create-menu-bar c menus)]
    (add-update-labels c menu-bar update-labels)
    menu-bar))

(defn create [context]
  (ui/table {:rows [[{:actor (ui/menu-bar->table (dev-menu* context (create-config context)))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (ui/label "")
                               (.setTouchable Touchable/disabled))
                      :expand? true
                      :fill-x? true
                      :fill-y? true}]]
             :fill-parent? true}))
