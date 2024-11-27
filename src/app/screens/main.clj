(ns app.screens.main
  (:require [app.world :as world]
            [clojure.gdx :as gdx]
            [forge.app :as app]
            [forge.db :as db]
            [forge.graphics.cursors :as cursors]
            [forge.input :refer [key-just-pressed?]]
            [forge.ui :as ui]
            [forge.ui.background-image :as background-image]
            [forge.utils :refer [dev-mode?]]))

(defn create []
  {:actors [(background-image/create)
            (ui/table
             {:rows
              (remove nil?
                      (concat
                       (for [world (db/all :properties/worlds)]
                         [(ui/text-button (str "Start " (:property/id world))
                                          #(world/start world))])
                       [(when dev-mode?
                          [(ui/text-button "Map editor"
                                           #(app/change-screen :screens/map-editor))])
                        (when dev-mode?
                          [(ui/text-button "Property editor"
                                           #(app/change-screen :screens/editor))])
                        [(ui/text-button "Exit"
                                         gdx/exit-app)]]))
              :cell-defaults {:pad-bottom 25}
              :fill-parent? true})
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :keys/escape)
                                (gdx/exit-app)))})]
   :screen (reify app/Screen
             (enter [_]
               (cursors/set :cursors/default))
             (exit [_])
             (render [_])
             (dispose [_]))})
