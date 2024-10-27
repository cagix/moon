(ns moon.widgets.dev-menu
  (:require [gdl.graphics]
            [gdl.graphics.camera :as cam]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [gdl.utils :refer [readable-number]]
            [moon.component :as component :refer [defc]]
            [moon.db :as db]
            [moon.graphics :as g]
            [moon.graphics.gui-view :as gui-view]
            [moon.graphics.world-view :as world-view]
            [moon.screen :as screen]
            [moon.world :as world])
  (:import (com.kotcrab.vis.ui.widget Menu MenuItem MenuBar)))

(defn- menu-item [text on-clicked]
  (doto (MenuItem. text)
    (.addListener (ui/change-listener on-clicked))))

(defn- add-upd-label
  ([table text-fn icon]
   (let [icon (ui/image->widget (g/image (str "images/" icon ".png")) {})
         label (ui/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (.addActor table (ui/actor {:act #(.setText label (text-fn))}))
     (.expandX (.right (.add table sub-table)))))
  ([table text-fn]
   (let [label (ui/label "")]
     (.addActor table (ui/actor {:act #(.setText label (text-fn))}))
     (.expandX (.right (.add table label))))))

(defn- add-debug-infos [mb]
  (let [table (.getTable mb)
        add! #(add-upd-label table %)]
    ;"Mouseover-Actor: "
    #_(when-let [actor (mouse-on-actor?)]
        (str "TRUE - name:" (.getName actor)
             "id: " (a/id actor)))
    (add-upd-label table
                   #(str "Mouseover-entity id: " (when-let [entity (world/mouseover-entity)] (:entity/id entity)))
                   "mouseover")
    (add-upd-label table
                   #(str "elapsed-time " (readable-number world/elapsed-time) " seconds")
                   "clock")
    (add! #(str "paused? " world/paused?))
    (add! #(str "GUI: " (gui-view/mouse-position)))
    (add! #(str "World: "(mapv int (world-view/mouse-position))))
    (add-upd-label table
                   #(str "Zoom: " (cam/zoom (world-view/camera)))
                   "zoom")
    (add! #(str "logic-frame: " world/logic-frame))
    (add-upd-label table
                   #(str "FPS: " (gdl.graphics/frames-per-second))
                   "fps")))

(defn- ->menu-bar []
  (let [menu-bar (MenuBar.)
        app-menu (Menu. "App")]
    (.addItem app-menu (menu-item "Map editor" (partial screen/change :screens/map-editor)))
    (.addItem app-menu (menu-item "Properties" (partial screen/change :screens/editor)))
    (.addItem app-menu (menu-item "Exit"       (partial screen/change :screens/main-menu)))
    (.addMenu menu-bar app-menu)
    (let [world (Menu. "World")]
      (doseq [{:keys [property/id]} (db/all :properties/worlds)]
        (.addItem world (menu-item (str "Start " id) #(world/start id))))
      (.addMenu menu-bar world))
    (let [help (Menu. "Help")]
        (.addItem help (MenuItem. "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"))
      (.addMenu menu-bar help))
    (def mb menu-bar)
    (add-debug-infos mb)
    menu-bar))

(defc :widgets/dev-menu
  (component/create [_]
    (ui/table {:rows [[{:actor (.getTable (->menu-bar))
                        :expand-x? true
                        :fill-x? true
                        :colspan 1}]
                      [{:actor (doto (ui/label "")
                                 (a/set-touchable! :disabled))
                        :expand? true
                        :fill-x? true
                        :fill-y? true}]]
               :fill-parent? true})))
