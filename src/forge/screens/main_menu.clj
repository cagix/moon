(ns forge.screens.main-menu
  (:require [clojure.gdx.app :as app]
            [clojure.gdx.input :refer [key-just-pressed?]]
            [clojure.utils :refer [dev-mode?]]
            [forge.app.cursors :refer [set-cursor]]
            [forge.app.db :as db]
            [forge.app.screens :refer [change-screen]]
            [forge.app.vis-ui :refer [ui-actor text-button] :as ui]
            [forge.screen :refer [Screen]]
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
                                          (change-screen :screens/world)
                                          (start-world world)))])
                        [(when dev-mode?
                           [(text-button "Map editor"
                                         #(change-screen :screens/map-editor))])
                         (when dev-mode?
                           [(text-button "Property editor"
                                         #(change-screen :screens/editor))])
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
