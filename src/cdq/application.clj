(ns cdq.application
  (:require [cdq.app]
            [cdq.core]
            [cdq.graphics :as graphics]
            [cdq.malli :as m]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx.utils Disposable)))

(def state (atom nil))

(defn post-runnable! [runnable]
  (swap! state cdq.app/add-runnable runnable)
  nil)

(q/defrecord Context [ctx/app
                      ctx/files
                      ctx/config
                      ctx/input
                      ctx/db
                      ctx/audio
                      ctx/stage
                      ctx/graphics
                      ctx/world])

(defn- create! [{:keys [audio files graphics input]} create-fns]
  (reset! state (reduce cdq.core/render*
                        (map->Context {:audio    audio
                                       :files    files
                                       :graphics graphics
                                       :input    input})
                        create-fns)))

; TODO call dispose! on all components
(defn- dispose! []
  (let [{:keys [ctx/audio
                ctx/graphics
                ctx/world]} @state]
    (Disposable/.dispose audio)
    (Disposable/.dispose graphics)
    (Disposable/.dispose (:world/tiled-map world))
    ; TODO vis-ui dispose
    ; TODO what else disposable?
    ; => :ctx/tiled-map definitely and also dispose when re-creting gamestate.
    ))

; TODO call resize! on all components
(defn- resize! [width height]
  (let [{:keys [ctx/graphics]} @state]
    (graphics/resize-viewports! graphics width height)))

(defn- render! []
  (swap! state (fn [ctx]
                 (reduce cdq.core/render*
                         ctx
                         (map requiring-resolve
                              '[cdq.app/validate
                                cdq.app/run-runnables!
                                cdq.render.debug-data-view/do!
                                cdq.render.assoc-active-entities/do!
                                cdq.render.set-camera-on-player/do!
                                cdq.render.clear-screen/do!
                                cdq.render.draw-world-map/do!
                                cdq.render.draw-on-world-viewport/do!
                                cdq.render.stage/do!
                                cdq.render.set-cursor/do!
                                cdq.render.player-state-handle-input/do!
                                cdq.render.update-mouseover-entity/do!
                                cdq.render.assoc-paused/do!
                                cdq.render.tick-world/do!
                                cdq.render.remove-destroyed-entities/do! ; do not pause as pickup item should be destroyed
                                cdq.render.camera-controls/do!
                                cdq.render.check-window-hotkeys/do!
                                cdq.app/validate])))))

(defn start! [{:keys [create-fns]}]
  (lwjgl/start-application! {:title "Cyber Dungeon Quest"
                             :windowed-mode {:width 1440 :height 900}
                             :foreground-fps 60}
                            {:create! (fn [context]
                                        (create! context create-fns))
                             :dispose! dispose!
                             :render! render!
                             :resize! resize!}))
