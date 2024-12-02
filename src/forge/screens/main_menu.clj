(ns forge.screens.main-menu
  (:require [forge.ui :as ui]
            [forge.screen :as screen]))

(defn create [background-image]
  {:actors [background-image
            (ui/table
             {:rows
              (remove nil?
                      (concat
                       (for [world (build-all :properties/worlds)]
                         [(ui/text-button (str "Start " (:property/id world))
                                          #(start-world world))])
                       [(when dev-mode?
                          [(ui/text-button "Map editor"
                                           #(change-screen :screens/map-editor))])
                        (when dev-mode?
                          [(ui/text-button "Property editor"
                                           #(change-screen :screens/editor))])
                        [(ui/text-button "Exit" exit-app)]]))
              :cell-defaults {:pad-bottom 25}
              :fill-parent? true})
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :keys/escape)
                                (exit-app)))})]
   :screen (reify screen/Screen
             (enter [_]
               (set-cursor :cursors/default))
             (exit [_])
             (render [_])
             (destroy [_]))})
