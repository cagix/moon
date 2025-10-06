(ns clojure.scene2d.actor)

(defprotocol Actor
  (get-stage [_])
  (get-x [_])
  (get-y [_])
  (get-name [_])
  (user-object [_])
  (set-user-object! [_ object])
  (visible? [_])
  (set-visible! [_ visible?])
  (set-touchable! [_ touchable])
  (remove! [_])
  (parent [_])
  (stage->local-coordinates [_ position])
  (hit [_ [x y]])
  (set-name! [_ name])
  (set-position! [_ x y])
  (get-width [_])
  (get-height [_])
  (add-listener! [_ listener]))

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))
