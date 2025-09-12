(ns clojure.scene2d.actor)

(defprotocol Actor
  (get-stage [actor])
  (get-x [actor])
  (get-y [actor])
  (get-name [actor])
  (user-object [actor])
  (set-user-object! [actor object])
  (visible? [actor])
  (set-visible! [actor visible?])
  (set-touchable! [actor touchable])
  (remove! [actor])
  (parent [actor])
  (stage->local-coordinates [actor position])
  (hit [actor [x y]]))

(defn toggle-visible! [actor]
  (set-visible! actor (not (visible? actor))))
