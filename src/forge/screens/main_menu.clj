(ns forge.screens.main-menu
  (:require [forge.ui :as ui]))

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
   :screen (reify Screen
             (screen-enter [_]
               (set-cursor :cursors/default))
             (screen-exit [_])
             (screen-render [_])
             (screen-destroy [_]))})
