(ns konstellate.style
  (:require
    garden.selectors
    [garden.core :as garden]
    [garden.def :refer [defkeyframes]]
    [konstellate.components.style :as components]))

(def shadow-color "rgba(46, 91, 255, 0.4)")
(def shadow "0 5px 25px 0 rgba(46, 91, 255, 0.4)")
(def left-shadow "5px 0px 25px 0 rgba(46, 91, 255, 0.4)")

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

(def Main [:.main {:display "flex"
                   :flex-direction "column"
                   :width "100%"
                   :height "100%"}
           [:.main-content {:display "flex"
                            :flex 1}
            [:.workspaces {:display "flex"
                           :flex-direction "column"
                           :height "100%"}
             [:icon {:font-size "18px"
                     :margin-right "8px"}]
             [:.add-workspace {:border-top (str "1px solid " border)
                               :cursor "pointer"
                               :display "flex"
                               :align-items "center"
                               :font-size "14px"
                               :padding "16px 0 16px 29px"}]]
            [:.graffle {:box-shadow left-shadow
                        :flex 1
                        :z-index 2}]]])

(def TitleBar
  [:.title-bar {:background "white"
                :border-bottom (str "1px solid " border)
                :display "flex"
                :align-items "center"
                :height "88px"
                :padding "32px"
                :position "relative"
                :z-index 3}
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
                    :font-size "14px"
                    :opacity 0
                    :padding "8px 0"
                    :position "absolute"
                    :min-width "128px"
                    :perspective "100px"
                    :transform-origin "50% 0"
                    :transition "transform 300ms ease, opacity 300ms ease"
                    :z-index 5}
   [:&.open {:opacity 1}]
   [:ol {:list-style-type "none"}
    [:li {:cursor "pointer"
          :padding "16px 0"
          :text-align "center"}
     [:&:hover {:background highlight}]]]])

(def SidePanel
  [:.side-panel {:background "white"
                 :height "100%"
                 :overflow "hidden"
                 :position "relative"
                 :width "32px"
                 :transition "width 500ms ease"}
   [:&.open {:width "256px"}
    [(garden.selectors/> "" :.side-panel-content) {:opacity 1}]]

   [:h4 {:letter-spacing "1.8px"
         :margin "16px 0 8px 40px"}]
   [:.side-panel-content {:height "100%"
                          :opacity 0
                          :width "256px"
                          :transition "opacity 500ms ease"}]
   [:.open-arrow {:cursor "pointer"
                  :position "absolute"
                  :font-size "14px"
                  :left 0
                  :text-align "center"
                  :top "18px"
                  :transform-origin "50% 50%"
                  :transition "transform 500ms ease"
                  :width "32px"
                  :z-index "2"}
    [:&.open {:transform "rotate(-180deg)"}]]])


(def WorkspaceLabel
  [:.workspace-label {:cursor "pointer"
                      :overflow "hidden"
                      :position "relative"}
   [".workspace-label-content:hover .close" {:display "block"}]
   [:.workspace-label-content {:background "white"
                               :font-size "14px"
                               :padding "4px 40px"
                               :position "relative"
                               :top "0"
                               :line-height "1.5em"
                               :transition "top 500ms ease"
                               :z-index 1}
    [:.label-standard-content {:display "flex"
                               :padding "8px 0"}]
    [:&.confirming {:top "56px"}]
    [:&.selected {:background "lightblue"}]
    [:.close {:display "none"}]
    [:icon {:cursor "pointer"
            :font-size "18px"}]
    [:.the-name {:flex 1}]]
   [:.confirm {:align-items "center"
               :background "red"
               :box-shadow (str "inset 0px 0px 25px " shadow-color)
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
            :margin-left "8px"
            :text-align "center"
            :width "72px"}]]])

(def WorkspaceList [:.workload-panel {:flex 1}])

(def styles
  [components/Button
   components/TextInput
   FloatingMenu
   Main
   TitleBar
   Reset
   WorkspaceLabel
   WorkspaceList
   SidePanel])

(defn spit-styles!
  []
  (spit "resources/public/css/style.css" (garden/css (concat [Reset] styles))))

