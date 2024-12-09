(ns forge.screens.main-menu
  (:require [anvil.app :as app]
            [anvil.db :as db]
            [anvil.graphics :refer [set-cursor]]
            [anvil.screen :refer [Screen]]
            [anvil.stage :as stage]
            [anvil.ui :refer [ui-actor text-button] :as ui]
            [clojure.gdx.input :refer [key-just-pressed?]]
            [clojure.utils :refer [dev-mode?]]
            [forge.world.create :refer [create-world]]))

(defn create []
  (stage/create
   {:actors [(ui/background-image)
             (ui/table
              {:rows
               (remove nil?
                       (concat
                        (for [world (db/build-all :properties/worlds)]
                          [(text-button (str "Start " (:property/id world))
                                        #(do
                                          (app/change-screen :screens/world)
                                          (create-world world)))])
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
