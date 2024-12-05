(ns forge.screens.main-menu
  (:require [forge.app :as app]
            [forge.core :refer :all]))

(defn create []
  {:actors [(background-image)
            (ui-table
             {:rows
              (remove nil?
                      (concat
                       (for [world (build-all :properties/worlds)]
                         [(text-button (str "Start " (:property/id world))
                                       #(start-world world))])
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
             (screen-enter [_]
               (set-cursor :cursors/default))
             (screen-exit [_])
             (screen-render [_])
             (screen-destroy [_]))})
