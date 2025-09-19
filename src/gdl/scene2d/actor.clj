(ns gdl.scene2d.actor)

(defprotocol Actor
  (act [_ delta f])
  (draw [_ f])
  (get-stage [_])
  (get-x [_])
  (get-y [_])
  (get-name [_])
  (get-width [_])
  (get-height [_])
  (user-object [_])
  (set-name! [_ name])
  (set-position! [_ x y])
  (set-user-object! [_ object])
  (visible? [_])
  (set-visible! [_ visible?])
  (set-touchable! [_ touchable])
  (remove! [_])
  (parent [_])
  (stage->local-coordinates [_ position])
  (hit [_ [x y]])
  (add-listener! [_ listener])
  (set-opts! [_ opts]))

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))

(defprotocol Tooltip
  (add-tooltip! [_ tooltip-text-or-text-fn])
  (remove-tooltip! [_]))
