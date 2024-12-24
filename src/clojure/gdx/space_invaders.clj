(ns clojure.gdx.space-invaders
  (:require [clojure.gdx.backends.lwjgl3 :as lwjgl3])
  (:import (com.badlogic.gdx ApplicationListener Gdx Input Input$Keys)
           (com.badlogic.gdx.graphics GL20 OrthographicCamera)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.utils ScreenUtils)))

;; TODO clojure.java.interop repo ... - or in 'clojure.reflect'

; clojure java interop patterns - proxy Ilookup, etc. , cool stuff, cool ideas
; guide for writing a clojure API for a java library

; TODO release as separate repo - example - ..

; TODO android / iOs - ask chatGPT for help ...
; TODO viewport
; TODO aliens move /
; TODO win/lose
; TODO music/sound
; TODO animations
; TODO score/screens ?
; TODO powerups ?
; TODO collision debug / pause / overlay FPS,etc. (dev-menu?)
; TODO alien/player/bullet width/height has to fit with image
; TODO exit ?
; TODO pass gdx app stuffs static with create as a map
; TODO render text, show controls ...
; TODO one version w. interop / one w. your 'API' ////....
; TODO dev-menu for game-stat map - depending on class can inspect ...
; I have this already ....

(def screen-width 800)
(def screen-height 600)

(defn create-texture [path]
  (Texture. (.internal Gdx/files path)))

(defn make-player []
  {:x (/ screen-width 2)
   :y 50
   :width 64
   :height 64
   :speed 300
   :texture (create-texture "ship/1.png")
   :bullets []})

(defn make-alien [x y]
  {:x x :y y :width 48 :height 48
   :texture (create-texture "alien/1.png")})

(defn make-game-state []
  {:player (make-player)
   :aliens (for [x (range 100 700 80)
                 y (range 400 500 60)]
             (make-alien x y))
   :bullets []
   :batch (SpriteBatch.)
   :camera (OrthographicCamera. screen-width screen-height)})

(defn update-player [player delta]
  (let [speed (:speed player)
        x (:x player)
        move-left (if (.isKeyPressed Gdx/input Input$Keys/A) (- x (* delta speed)) x)
        move-right (if (.isKeyPressed Gdx/input Input$Keys/D) (+ x (* delta speed)) move-left)
        new-x (max 0 (min move-right (- screen-width (:width player))))]
    (assoc player :x new-x)))

(defn update-bullets [bullets delta]
  (->> bullets
       (map (fn [bullet]
              (update bullet :y #(+ % (* 500 delta)))))
       (filter #(> (:y %) 0))))

(defn check-collision [alien bullet]
  (and (< (:x bullet) (+ (:x alien) (:width alien)))
       (> (+ (:x bullet) 8) (:x alien))
       (< (:y bullet) (+ (:y alien) (:height alien)))
       (> (+ (:y bullet) 16) (:y alien))))

(defn update-aliens [aliens bullets]
  (reduce
   (fn [acc alien]
     (if (some #(check-collision alien %) bullets)
       acc
       (conj acc alien)))
   []
   aliens))

(defn handle-input [game-state]
  (if (.isKeyJustPressed Gdx/input Input$Keys/SPACE)
    (let [player (:player game-state)
          bullet {:x (+ (:x player) 28) :y (+ (:y player) 64)}]
      (update game-state :bullets conj bullet))
    game-state))

(defn render-entity [batch entity]
  (.draw batch
         (:texture entity)
         (float (:x entity))
         (float (:y entity))))

(defn render-game [game-state]
  (ScreenUtils/clear 0 0 0 1)
  (let [batch (:batch game-state)
        player (:player game-state)
        aliens (:aliens game-state)
        bullets (:bullets game-state)]
    (.begin batch)
    (render-entity batch player)
    (doseq [alien aliens]
      (render-entity batch alien))
    (doseq [bullet bullets]
      (render-entity batch {:texture (create-texture "bullet.png") :x (:x bullet) :y (:y bullet)}))
    (.end batch)))

(defn update-game [game-state delta]
  (-> game-state
      handle-input
      (update :player update-player delta)
      (update :bullets update-bullets delta)
      (update :aliens #(update-aliens % (:bullets game-state)))))

(def state (atom nil))

(def gdx-stuff nil)

(defn game-listener []
  (reify lwjgl3/Application
    (create [_ gdx-state]
      (def gdx-stuff gdx-state)
      (reset! state (make-game-state))
      (swap! state assoc :camera (OrthographicCamera. screen-width screen-height)))

    (dispose [_]
      (doseq [texture (concat [(-> @state :player :texture)]
                              (map :texture (:aliens @state)))]
        (.dispose texture)))

    (render [_]
      (let [delta (.getDeltaTime Gdx/graphics)]
        (swap! state update-game delta)
        (render-game @state)))

    (resize [_ width height]
      (.setToOrtho (:camera @state) false))))

(defn -main []
  (lwjgl3/start {:title "Space Invaders"
                 :width screen-width
                 :height screen-height
                 :fps 60}
                (game-listener)))
