(ns anvil.widgets.dev-menu
  (:require [anvil.controls :as controls]
            [anvil.db :as db]
            [anvil.widgets :as widgets]
            [anvil.world :as world]
            [gdl.context :as ctx]
            [gdl.graphics :as g]
            [gdl.graphics.camera :as cam]
            [gdl.ui :as ui :refer [ui-actor]]
            [gdl.ui.group :refer [add-actor!]])
  (:import (com.badlogic.gdx.scenes.scene2d Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Table)))

(defn- menu-item [text on-clicked]
  (doto (ui/menu-item text)
    (.addListener (ui/change-listener on-clicked))))

(defn- add-upd-label
  ([table text-fn icon]
   (let [icon (ui/image->widget (ctx/sprite icon) {})
         label (ui/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (add-actor! table (ui-actor {:act #(.setText label (str (text-fn)))}))
     (.expandX (.right (Table/.add table sub-table)))))
  ([table text-fn]
   (let [label (ui/label "")]
     (add-actor! table (ui-actor {:act #(.setText label (str (text-fn)))}))
     (.expandX (.right (Table/.add table label))))))

(defn- add-update-labels [menu-bar update-labels]
  (let [table (ui/menu-bar->table menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn))]
        (if icon
          (add-upd-label table update-fn icon)
          (add-upd-label table update-fn))))))

(defn- add-menu [menu-bar {:keys [label items]}]
  (let [app-menu (ui/menu label)]
    (doseq [{:keys [label on-click]} items]
      (.addItem app-menu (menu-item label (or on-click (fn [])))))
    (ui/add-menu menu-bar app-menu)))

(defn- create-menu-bar [menus]
  (let [menu-bar (ui/menu-bar)]
    (run! #(add-menu menu-bar %) menus)
    menu-bar))

(defn dev-menu* [{:keys [menus update-labels]}]
  (let [menu-bar (create-menu-bar menus)]
    (add-update-labels menu-bar update-labels)
    menu-bar))

;"Mouseover-Actor: "
#_(when-let [actor (stage/mouse-on-actor?)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn dev-menu-table [config]
  (ui/table {:rows [[{:actor (ui/menu-bar->table (dev-menu* config))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (ui/label "")
                               (.setTouchable Touchable/disabled))
                      :expand? true
                      :fill-x? true
                      :fill-y? true}]]
             :fill-parent? true}))

(defn uf-dev-menu-table []
  (dev-menu-table
   {:menus [{:label "Menu1"
             :items [{:label "Button1"
                      :on-click (fn [])}]}]
    :update-labels [{:label "GUI"
                     :update-fn g/mouse-position}
                    {:label "World"
                     :update-fn #(mapv int (g/world-mouse-position))}
                    {:label "Zoom"
                     :update-fn #(cam/zoom g/camera)
                     :icon "images/zoom.png"}
                    {:label "FPS"
                     :update-fn g/frames-per-second
                     :icon "images/fps.png"}]}))

(defn- config []
  {:menus [{:label "World"
            :items (for [world (db/build-all :properties/worlds)]
                     {:label (str "Start " (:property/id world))
                      :on-click #(world/create (:property/id world))})}
           {:label "Help"
            :items [{:label controls/help-text}]}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn #(when-let [entity (world/mouseover-entity)]
                                  (:entity/id entity))
                    :icon "images/mouseover.png"}
                   {:label "elapsed-time"
                    :update-fn #(str (readable-number world/elapsed-time) " seconds")
                    :icon "images/clock.png"}
                   {:label "paused?"
                    :update-fn (fn [] world/paused?)}
                   {:label "GUI"
                    :update-fn g/mouse-position}
                   {:label "World"
                    :update-fn #(mapv int (g/world-mouse-position))}
                   {:label "Zoom"
                    :update-fn #(cam/zoom g/camera)
                    :icon "images/zoom.png"}
                   {:label "FPS"
                    :update-fn g/frames-per-second
                    :icon "images/fps.png"}]})

(defn-impl widgets/dev-menu []
  (dev-menu-table (config)))
