(ns cdq.application
  (:require [gdl.application :as application]))

; I rewrote states exit/enter/etc
; How do I know it even workds still ?

; => no tests ...
; => tests improve design

; * SEPARATE WIRING FROM LOGIC
; * MOVE LOGIC TO ITS PLACE (SEE FORM DEPENDENCIES)

; grep defn-\|private

(def create-fns '[cdq.application.create.config/do!
                  cdq.application.create.requires/do!
                  cdq.application.create.db/do!
                  cdq.application.create.assets/do!
                  cdq.application.create.batch/do!
                  cdq.application.create.shape-drawer-texture/do!
                  cdq.application.create.shape-drawer/do!
                  cdq.application.create.cursors/do!
                  cdq.application.create.default-font/do!
                  cdq.application.create.world-unit-scale/do!
                  cdq.application.create.world-viewport/do!
                  cdq.application.create.tiled-map-renderer/do!
                  cdq.application.create.ui-viewport/do!
                  cdq.application.create.ui/do!
                  cdq.application.create.game-state/do!])

(def dispose-fn 'cdq.application.dispose/do!)

(def resize-fn 'cdq.application.resize/do!)

(def render-fns '[cdq.application.render.bind-active-entities/do!
                  cdq.application.render.set-camera-on-player/do!
                  cdq.application.render.clear-screen/do!
                  cdq.application.render.draw-tiled-map/do!
                  cdq.application.render.draw-on-world-viewport/do!
                  cdq.application.render.draw-ui/do!
                  cdq.application.render.update-ui/do!
                  cdq.application.render.player-state-handle-click/do!
                  cdq.application.render.update-mouseover-entity/do!
                  cdq.application.render.bind-paused/do!
                  cdq.application.render.when-not-paused/do!
                  cdq.application.render.remove-destroyed-entities/do! ; do not pause as pickup item should be destroyed
                  cdq.application.render.camera-controls/do!])

(comment
 (defn create! [initial-context create-fns]
   (reduce (fn [ctx create-fn]
             (if (vector? create-fn)
               (let [[k [f & params]] create-fn]
                 (assoc ctx k (apply f ctx params)))
               (do
                (create-fn ctx)
                ctx)))
           initial-context
           create-fns))

 ; Issues which might appear:
 ; * ui needs to query app-state, cannot to simple transformations
 ; without using a StageWithObject
 ; @ cdq.ui.player-state-draw -> inside actor draw

 ; * also @ graphics ... always pass to 'draw' stuff ...

 ; * audiovisual build from db, ...
 ; * always timer create

 ; TODO also remember actor needs to call (super) from 'act' thath's why maybe ui
 ; problems of tooltips?

 ; * Pass @ tx

 ; - editor altering db
 ; - bind-root / alter var time , etc.
 ; => swap! state ?

 ; => It might not be worth it
 ; => Create a design document

 )

(def config
  {:title "Cyber Dungeon Quest"
   :window-width 1440
   :window-height 900
   :fps 60
   :dock-icon "moon.png"
   :create! (fn []
              (comment

               (reset! state (create! {} create-fns))

               )
              (doseq [f create-fns]
                ((requiring-resolve f))))
   :dispose! (fn []
               ((requiring-resolve dispose-fn)))
   :render! (fn []
              (doseq [f render-fns]
                ((requiring-resolve f))))
   :resize! (fn [_width _height]
              ((requiring-resolve resize-fn)))})

(defn -main []
  (application/start! config))
