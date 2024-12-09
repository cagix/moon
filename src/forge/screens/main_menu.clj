(ns forge.screens.main-menu
  (:require [anvil.app :as app]
            [anvil.graphics :refer [set-cursor]]
            [anvil.screen :refer [Screen]]
            [clojure.gdx.input :refer [key-just-pressed?]]
            [clojure.utils :refer [dev-mode?]]
            [forge.app.db :as db]
            [forge.app.vis-ui :refer [ui-actor text-button] :as ui]
            [forge.screens.stage :as stage]
            [forge.screens.world :refer [start-world]]
            [forge.ui :refer [background-image]]))

(defn create []
  (stage/create
   {:actors [(background-image)
             (ui/table
              {:rows
               (remove nil?
                       (concat
                        (for [world (db/build-all :properties/worlds)]
                          [(text-button (str "Start " (:property/id world))
                                        #(do
                                          (app/change-screen :screens/world)
                                          (start-world world)))])
                        [(when dev-mode?
                           [(text-button "Map editor"
                                         #(app/change-screen :screens/map-editor))])
                         (when dev-mode?
                           [(text-button "Property editor"
                                         #(app/change-screen :screens/editor))])
                         [(text-button "Exit" app/exit)]]))
               :cell-defaults {:pad-bottom 25}
               :fill-parent? true})
             (ui-actor {:act (fn []
                               (when (key-just-pressed? :keys/escape)
                                 (app/exit)))})]
    :screen (reify Screen
              (enter [_]
                (set-cursor :cursors/default))
              (exit [_])
              (render [_])
              (dispose [_]))}))
