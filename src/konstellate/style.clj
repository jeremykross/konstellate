(ns konstellate.style
  (:require
    garden.selectors
    [garden.core :as garden]
    [garden.def :refer [defkeyframes]]
    [konstellate.components.style :as components]))

(def shadow "0 5px 25px 0 rgba(46, 91, 255, 0.4)")
(def primary "#FEA7BD")
(def secondary "#09EDC8")
(def grey "#f4f6fc")
(def highlight "#00a2ff")
(def text "#081018")
(def border "#d9e1ff")

(defkeyframes FadeInAnim
  [:from {:opacity 0
          :transform "scale(0.95, 0.95)"}]
  [:to {:opacity 1
        :transform "scale(1, 1)"}])

(def Reset
  [[:html :body {:width "100%"
                 :height "100%"}]
   [:body {:background grey
           :box-sizing "border-box"
           :color text
           :font-style "sans-serif"
           :font-family "'Rubik', sans-serif"
           :margin 0
           :padding 0
           :width "100%"
           :height "100%"}]
   [:h1 {:font-size "32px"
         :letter-spacing "4px"}]
   [:#app {:width "100%"
           :height "100%"}]
   [:body [:* {:box-sizing "border-box"
               :margin 0
               :padding 0}]]])

(def TitleBar
  [:.title-bar {:background "white"
                :border-bottom (str "1px solid " border)
                :display "flex"
                :align-items "center"
                :height "88px"
                :padding "32px"
                :position "relative"}
   [:.workspace {:cursor "pointer"
                 :font-weight "bold"
                 :position "absolute"
                 :text-align "center"
                 :width "100%"}]
   [:.more {:cursor "pointer"
            :display "block"
            :margin-left "auto"}]])
                

(def FloatingMenu
  [:.floating-menu {:background "white"
                    :border (str "1px solid " border)
                    :border-radius "4px"
                    :box-shadow shadow
                    :opacity 0
                    :padding "8px 0"
                    :position "absolute"
                    :margin "32px"
                    :min-width "256px"
                    :perspective "100px"
                    :transform "rotateX(32deg)"
                    :transform-origin "50% 0"
                    :transition "transform 300ms ease, opacity 300ms ease"}
   [:&.open {:opacity 1
             :transform "rotateX(0deg)"}]
   [:ol {:list-style-type "none"}
    [:li {:cursor "pointer"
          :font-weight "bold"
          :padding "16px"
          :text-align "center"}
     [:&:hover {:background highlight}]]]])

(def WorkspacePanel
  [:.workspace-panel {}])

(def WorkspaceLabel
  [:.workspace-label {:overflow "hidden"
                      :position "relative"}
   [:.content {:background "white"
               :display "flex"
               :font-weight "bold"
               :padding "16px 32px"
               :position "relative"
               :top "0"
               :line-height "1.5em"
               :transition "top 500ms ease"
               :z-index 1}
    [:&.confirming {:top "56px"}]
    [:icon {:cursor "pointer"}]
    [:.the-name {:flex 1
                 :margin-left "8px"}]]
   [".content:hover .more" {:display "none"}]
   [:.confirm {:align-items "center"
               :background "red"
               :color "white"
               :display "flex"
               :left 0
               :line-height "1.5em"
               :font-weight "bold"
               :padding "0 32px"
               :position "absolute"
               :top 0
               :width "100%"
               :height "100%"}
    [:label {:margin-right "auto"}]
    [:.btn {:border "1px solid white"
            :border-radius "4px"
            :cursor "pointer"
            :margin-left "16px"
            :text-align "center"
            :width "72px"}]]])


(def styles
  [components/Button
   FloatingMenu
   TitleBar
   Reset
   WorkspaceLabel
   WorkspacePanel])

(defn spit-styles!
  []
  (spit "resources/public/css/style.css" (garden/css (concat [Reset] styles))))

