(ns forge.entity.render
  (:require [forge.entity :as entity]
            [forge.entity.active :refer [check-update-ctx]]
            [forge.entity.components :refer [hitpoints enemy]]
            [forge.entity.player.item-on-cursor :refer [item-place-position world-item?]]
            [forge.graphics :refer [draw-filled-circle draw-image draw-sector draw-text draw-filled-rectangle pixels->world-units draw-rotated-centered draw-line with-line-width draw-ellipse draw-circle draw-centered]]
            [forge.val-max :as val-max]
            [forge.world :refer [player-eid finished-ratio]]))

(defmethod entity/render-below :stunned [_ entity]
  (draw-circle (:position entity) 0.5 [1 1 1 0.6]))

(defmethod entity/render-above :entity/string-effect [[_ {:keys [text]}] entity]
  (let [[x y] (:position entity)]
    (draw-text {:text text
                :x x
                :y (+ y (:half-height entity) (pixels->world-units 5))
                :scale 2
                :up? true})))

(defmethod entity/render-below :player-item-on-cursor [[_ {:keys [item]}] entity]
  (when (world-item?)
    (draw-centered (:entity/image item)
                   (item-place-position entity))))

(defmethod entity/render-above :npc-sleeping [_ entity]
  (let [[x y] (:position entity)]
    (draw-text {:text "zzz"
                :x x
                :y (+ y (:half-height entity))
                :up? true})))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethod entity/render-below :entity/mouseover? [_ {:keys [entity/faction] :as entity}]
  (let [player @player-eid]
    (with-line-width 3
      #(draw-ellipse (:position entity)
                     (:half-width entity)
                     (:half-height entity)
                     (cond (= faction (enemy player))
                           enemy-color
                           (= faction (:entity/faction player))
                           friendly-color
                           :else
                           neutral-color)))))

(defmethod entity/render :entity/line-render [[_ {:keys [thick? end color]}] entity]
  (let [position (:position entity)]
    (if thick?
      (with-line-width 4
        #(draw-line position end color))
      (draw-line position end color))))

(defmethod entity/render :entity/image [[_ image] entity]
  (draw-rotated-centered image
                         (or (:rotation-angle entity) 0)
                         (:position entity)))

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (draw-filled-circle center radius [1 1 1 0.125])
    (draw-sector center radius
                 90 ; start-angle
                 (* (float action-counter-ratio) 360) ; degree
                 [1 1 1 0.5])
    (draw-image image [(- (float x) radius) y])))

; TODO draw opacity as of counter ratio?
(defmethod entity/render-above :entity/temp-modifier [_ entity]
  (draw-filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4]))

(defmethod entity/render-info :active-skill [[_ {:keys [skill effect-ctx counter]}] entity]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image image entity (:position entity) (finished-ratio counter))
    (effects-render (check-update-ctx effect-ctx) effects)))

(defmethod entity/render :entity/clickable [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity}]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (draw-text {:text text
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))

(def ^:private hpbar-colors
  {:green     [0 0.8 0]
   :darkgreen [0 0.5 0]
   :yellow    [0.5 0.5 0]
   :red       [0.5 0 0]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn- draw-hpbar [{:keys [position width half-width half-height]}
                   ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (pixels->world-units 5)
          border (pixels->world-units borders-px)]
      (draw-filled-rectangle x y width height black)
      (draw-filled-rectangle (+ x border)
                             (+ y border)
                             (- (* width ratio) (* 2 border))
                             (- height (* 2 border))
                             (hpbar-color ratio)))))

(defmethod entity/render-info :entity/hp [_ entity]
  (let [ratio (val-max/ratio (hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar entity ratio))))
