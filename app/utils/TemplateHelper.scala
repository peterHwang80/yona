package utils

import org.apache.commons.lang3.{ArrayUtils, StringUtils}
import play.mvc.{Call, Http}
import org.joda.time.DateTimeConstants
import org.apache.commons.io.FilenameUtils
import play.i18n.Messages
import controllers.{Application, UserApp, routes}
import views.html._
import java.net.URI

import playRepository.DiffLine
import playRepository.DiffLineType
import models.CodeRange.Side
import models._
import java.net.URLEncoder
import java.util
import java.util.Date

import scala.annotation.tailrec
import playRepository.FileDiff
import play.api.i18n.Lang
import play.twirl.api.Html

import collection.convert.wrapAll._
import scala.util.control.Breaks._

object TemplateHelper {
  def isAllowedOAuthProvider(provider: String): Boolean = {
    val allowedProviders = play.Configuration.root.getString("application.social.login.support", "").replaceAll(" ", "").split(",")
    allowedProviders.toStream.contains(provider)
  }

  def showWatchers(posting: AbstractPosting): String = {
      "<div class='show-watchers' data-toggle='tooltip' data-placement='top' data-trigger='hover' data-html='true' title='" + Messages.get("watchers") + "'>" +
      "<button id='watcher-list-button' type='button' class='ybtn'><i class='yobicon-emo-happy'></i><span class='watcherCount'></span></button>" +
      "</div>"
  }

  def GithubLogo: String = {"""<span class="github"><svg aria-hidden="true" height="24" version="1.1" viewBox="0 0 16 16" width="19"><path
  d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59 0.4 0.07 0.55-0.17 0.55-0.38 0-0.19-0.01-0.82-0.01-1.49-2.01 0.37-2.53-0.49-2.69-0.94-0.09-0.23-0.48-0.94-0.82-1.13-0.28-0.15-0.68-0.52-0.01-0.53 0.63-0.01 1.08 0.58 1.23 0.82 0.72 1.21 1.87 0.87 2.33 0.66 0.07-0.52 0.28-0.87 0.51-1.07-1.78-0.2-3.64-0.89-3.64-3.95 0-0.87 0.31-1.59 0.82-2.15-0.08-0.2-0.36-1.02 0.08-2.12 0 0 0.67-0.21 2.2 0.82 0.64-0.18 1.32-0.27 2-0.27 0.68 0 1.36 0.09 2 0.27 1.53-1.04 2.2-0.82 2.2-0.82 0.44 1.1 0.16 1.92 0.08 2.12 0.51 0.56 0.82 1.27 0.82 2.15 0 3.07-1.87 3.75-3.65 3.95 0.29 0.25 0.54 0.73 0.54 1.48 0 1.07-0.01 1.93-0.01 2.2 0 0.21 0.15 0.46 0.55 0.38C13.71 14.53 16 11.53 16 8 16 3.58 12.42 0 8 0z"></path></svg></span>"""}

  def GoogleLogo: String = {
    val url = routes.Assets.at("images/provider-logo/btn_google_light_normal_ios.svg")
    s"""<span class="google"><img src="$url"></span>"""
  }

  def providerWithLogo(provider:String): String = {
    val googleLogo = routes.Assets.at("images/provider-logo/btn_google_light_normal_ios.svg")
    provider match {
      case "github" => s"""<span class="auth-provider-logo">$GithubLogo <span class="provider-name">Sign in with ${Application.GITHUB_NAME}</span></span>"""
      case "google" => s"""<span class="auth-provider-logo"><img src="$googleLogo" alt="login with Google"> Sign in with Google</span>"""
      case _ => ""
    }
  }

  def buildQueryString(call: Call, queryMap: Map[String, String]): String = {
    val baseUrl = call.toString
    var prefix = "?"
    var query = ""
    if ((baseUrl indexOf "?") != -1) {
      prefix = "&"
    }
    queryMap.map {
      v => query += v._1 + "=" + v._2 + "&"
    }
    baseUrl + prefix + query.dropRight(1)
  }

  def buildAttrString(attrMap: java.util.Map[String, String]): String = {
    var attr = ""
    attrMap.map {
      v => attr += v._1 + "=" + v._2 + " "
    }
    attr.dropRight(1)
  }

  def agoString(duration: org.joda.time.Duration) = {
    if (duration != null){
      val sec = duration.getMillis / DateTimeConstants.MILLIS_PER_SECOND

      sec match {
        case x if x >= 86400 => plural("common.time.day", duration.getStandardDays)
        case x if x >= 3600 => plural("common.time.hour", duration.getStandardHours)
        case x if x >= 60 => plural("common.time.minute", duration.getStandardMinutes)
        case x if x > 0 => plural("common.time.second", duration.getStandardSeconds)
        case x if x == null => ""
        case _ => Messages.get("common.time.just")
      }
    } else {
      ""
    }
  }

  def agoOrDateString(date: java.util.Date) = {
    var year = JodaDateUtil.getDateString(date, "yyyy")
    var thisYear = JodaDateUtil.getDateString(new Date(), "yyyy")
    val ago = JodaDateUtil.ago(date)
    if (ago.getStandardDays < 8) {
      agoString(ago)
    } else if (thisYear.equals(year)) {
      JodaDateUtil.getDateString(date, "MM-dd")
    } else {
      JodaDateUtil.getDateString(date, "yyyy-MM-dd")
    }
  }

  def plural(key: String, count: Number): String = {
    var _key = key
    if (count != 1) _key = key + "s"
    Messages.get(_key, count.toString)
  }

  def urlToPicture(email: String, size: Int = 64) = {
    GravatarUtil.getAvatar(email, size)
  }

  def simpleForm(elements: helper.FieldElements) = {
    elements.input
  }

  def getJSPath: String = {
    routes.Assets.at("javascripts/").toString
  }

  def nullOrEquals(a: String, b: String) = (a == null || b == null) ||  a.equals(b)

  def ifElse(condition:Boolean, a: String, b: String): String = {
    if(condition){
      a
    } else {
      b
    }
  }

  def equalsThen(a: String, b: String, thenStr: String): String = {
    if(a != null && b != null && StringUtils.equals(a, b)){
      thenStr
    } else {
      ""
    }
  }

  def getPort(uri: URI) = {
    val port = uri.getPort
    port match {
      case -1 => uri.toURL.getDefaultPort
      case _ => port
    }
  }

  // Whether the given uris are pointing the same resource.
  def resourceEquals(a: URI, b: URI) =
    nullOrEquals(a.getHost, b.getHost) && getPort(a) == getPort(b) && StringUtils.equals(a.getPath, b.getPath)

  // Get the url to return to the list page from the view page.
  // Return the referrer if the it is the uri for the list page, an/ return the
  // default uri if not.
  def urlToList(referrer: String, defaultURI: String) = {
    def fullURI(u: String) = Config.createFullURI(u).normalize
    referrer match {
      case (uri: String) if resourceEquals(fullURI(uri), fullURI(defaultURI)) => uri
      case (_) => defaultURI
    }
  }

  def getUserAvatarUrl(user: models.User, avatarSize: Int): String = {
    if (user.avatarUrl == UserApp.DEFAULT_AVATAR_URL) {
      urlToPicture(user.email, avatarSize)
    } else {
      user.avatarUrl
    }
  }

  def getUserAvatar(user: models.User, avatarSize:String = "small"): String = {
    user.refresh()
    var userInfoURL = routes.UserApp.userInfo(user.loginId).toString()

    "<a href=\"" + userInfoURL + "\" class=\"usf-group\" data-toggle=\"tooltip\" data-placement=\"top\" title=\"" + user.name + "\"><img src=\"" + user.avatarUrl + "\" class=\"avatar-wrap " + avatarSize + "\"></a>"
  }

  def urlToProjectBG(project: Project) = {
    models.Attachment.findByContainer(project.asResource) match {
      case files if files.size > 0 => routes.AttachmentApp.getFile(files.head.id)
      case _ => routes.Assets.at("images/project_default.jpg")
    }
  }

  def urlToProjectLogo(project: Project) = {
    models.Attachment.findByContainer(project.asResource) match {
      case files if files.size > 0 => routes.AttachmentApp.getFile(files.head.id)
      case _ => routes.Assets.at("images/project_default_logo.png")
    }
  }

  def hasProjectLogo(project: Project) = {
    models.Attachment.findByContainer(project.asResource) match {
      case files if files.size > 0 => true
      case _ => false
    }
  }

  /**
   * get branch item name
   * @param branch
   * @return
   */
  def branchItemName(branch:String) = {
    Branches.itemName(branch)
  }

  object Branches {
    def itemType(branch: String): String = {
      val names = branch.split('/').toList

      names match {
        case "refs" :: "heads" :: _ => "branch"
        case "refs" :: "tags"  :: _ => "tag"
        case "refs" :: name    :: _ => name
        case _ => branch
      }
    }

    def itemName(branch: String): String = {
      val names = branch.split("/", 3).toList

      names match {
        case "refs" :: branchType :: branchName => branchName(0)
        case _ => branch
      }
    }

    def branchInHTML(branch: String) = {
      val names = branch.split('/')
      val branchType = itemType(branch)
      val branchName = itemName(branch)

      if(names(0).equals("refs") && names.length >= 3){
        "<span class=\"label " + branchType + "\">" + branchType + "</span>" + branchName
      } else {
        branch
      }
    }

    def getURL(viewType:String, project:Project, branchName:String, path:String) = viewType match {
      case _ =>
        "#"
    }

    def getItemHTML(viewType:String, project:Project, branch:String, path:String, selectedBranch:String): String = {
      "<li data-value=\"" + branch + "\"" +
        equalsThen(branch, selectedBranch , "data-selected=\"true\"") + ">" +
        "<a href=\"" + getURL(viewType, project, itemName(branch), path) + "\">" +
        branchInHTML(branch) + "</a>" +
        "</li>"
    }
  }

  def urlToCompare(project: Project, compare: String) = {
    routes.ProjectApp.project(project.owner, project.name).url
  }

  def getPercent(unit:Double, total:Double) = {
    ((unit / total) * 100).toInt
  }

  def makeIssuesLink(project:Project, param:Map[String,String]) = {
      buildQueryString(
          routes.IssueApp.issues(project.owner, project.name, "open"),
          param
      )
  }

  def urlToOrganizationLogo(organization: Organization) = {
    models.Attachment.findByContainer(organization.asResource) match {
      case files if files.size > 0 => routes.AttachmentApp.getFile(files.head.id)
      case _ => routes.Assets.at("images/group_default.png")
    }
  }

  def hasOrganizationLogo(organization: Organization) = {
    models.Attachment.findByContainer(organization.asResource) match {
      case files if files.size > 0 => true
      case _ => false
    }
  }

  object DiffRenderer {
    def isAuthorComment(commentId: String) = if(commentId == UserApp.currentUser().loginId) "author"

    def shortId(commitId: String) = commitId.substring(0, Math.min(7, commitId.size))

    def urlToCommentThread(thread: CommentThread) = {
      val container = urlToContainer(thread)
      if (container == "#") {
        "#"
      } else {
        container + "#thread-" + thread.id
      }
    }

    def urlToContainer(thread: CommentThread) = {
      if (thread == null || thread.project == null) {
        "#"
      } else {
        thread.project.refresh()
        routes.ProjectApp.project(thread.project.owner, thread.project.name).url
      }
    }

    def urlToPostNewComment(thread: CommentThread) = {
      if (thread == null || thread.project == null) {
        "#"
      } else {
        thread.project.refresh()
        routes.ProjectApp.project(thread.project.owner, thread.project.name)
      }
    }

    def getResourceType(thread: CommentThread) = {
      if(thread != null && thread.isOnPullRequest){
        models.enumeration.ResourceType.REVIEW_COMMENT
      } else {
        models.enumeration.ResourceType.COMMIT_COMMENT
      }
    }
  }

  object CodeBrowser {
    def fieldText(obj:com.fasterxml.jackson.databind.JsonNode, field:String):String = {
      if(obj.get(field) != null){
        obj.get(field).textValue()
      } else {
        ""
      }
    }

    def getDataPath(listPath:String, fileName:String):String = {
      if(listPath == ""){
        fileName
      }else{
        getCorrectedPath(listPath, fileName)
      }
    }

    def getUserLink(userLoginId:String):String = {
      if(userLoginId != null && userLoginId != ""){
        "/" + userLoginId
      } else {
        "javascript:void(); return false;"
      }
    }

    def getAvatar(file:com.fasterxml.jackson.databind.JsonNode):String = {
      val avatarURL = fieldText(file, "avatar")

      if(avatarURL != null){
        "<a href=\"" + getUserLink(fieldText(file, "userLoginId")) + "\" class=\"avatar-wrap smaller\"><img src=\"" + avatarURL + "\"></a>"
      } else {
        ""
      }
    }

    def getFileClass(file:com.fasterxml.jackson.databind.JsonNode):String = {
      if(fieldText(file, "name") == ".."){
        "updir"
      } else {
        fieldText(file, "type") match {
          case "folder" => "dynatree-ico-cf"
          case _ =>        "dynatree-ico-c"
        }
      }
    }

    def getFileDate(file:com.fasterxml.jackson.databind.JsonNode, field:String)(implicit lang:Lang):String = {
      JodaDateUtil.momentFromNow(file.get(field).longValue, lang.language)
    }

    def getFileAgoOrDate(file:com.fasterxml.jackson.databind.JsonNode, field:String) = {
      agoOrDateString(new java.util.Date(file.get(field).longValue))
    }

    def getCorrectedPath(filePath:String, fileName:String):String = {
      if(StringUtils.isNotEmpty(filePath) && (filePath.substring(filePath.length() - 1) == "/")){
        filePath + getEncodeEachPathName(fileName)
      } else {
        filePath + "/" + getEncodeEachPathName(fileName)
      }
    }

    def getEncodeEachPathName(path: String): String ={
      val paths = path.split("/")
      var encodedPaths = new Array[String](paths.length)
      for ( i <- 0 until paths.length ) {
        encodedPaths(i) = HttpUtil.encodeUrlString(paths(i))
      }
      encodedPaths.mkString("/")
    }

    def getFileRev(vcsType:String, file:com.fasterxml.jackson.databind.JsonNode):String = {
      vcsType match {
        case "GIT" => fieldText(file,"commitId")
        case "Subversion" => fieldText(file, "revisionNo")
        case _ => ""
      }
    }
  }

  def countHtml(icon:String, link:String, count: Int, strong:String = "") = {
      Html("""<a href="%s"><span class="count-groups item-icon %s">
        <i class="yobicon-%s"></i>
      </span>
      <span class="count-groups item-count %s">
        %d
      </span></a> """.format(link, strong, icon, strong, count))
  }

  def isMarkdownExtension(path: String):Boolean = {
    var ext = FilenameUtils.getExtension(path).toLowerCase()
    var markdownExtenstions = List("markdown", "mdown", "mkdn", "mkd", "md", "mdwn")

    markdownExtenstions.contains(ext)

  }

  def showHeaderWordsInBracketsIfExist(title: String) = {
    val prefixes =  new StringBuilder
    for(prefix <- extractHeaderWordsInBrackets(title) if prefix.trim.indexOf("[") == 0 ){
      if(prefix.contains("]")){
        prefixes.append("<a href='javascript:void(0)' class='title-prefix'>" + prefix.trim + "</a>")
      }
    }

    if(!madeByHeaderWordsOnly(title)){
      Html(prefixes.toString)
    }
  }
  
  def removeHeaderWords(title: String):String = {
    if(madeByHeaderWordsOnly(title)){
      return title
    } else {
      return title.replace(findHeaderWords(title).toString(),"")
    }
  }

  private def findHeaderWords(title: String): StringBuilder = {
    val prefixes = new StringBuilder
    for (prefix <- extractHeaderWordsInBrackets(title) if prefix.trim.indexOf("[") == 0) {
      if (prefix.contains("]")) {
        prefixes.append(prefix)
      }
    }
    prefixes
  }

  private def madeByHeaderWordsOnly(title: String): Boolean = {
    title.trim.indexOf("]") + 1 == title.trim.length ||
      StringUtils.isEmpty(title.replace(findHeaderWords(title),"").trim)
  }

  def extractHeaderWordsInBrackets(title: String): Array[String] = {
    return title.split("(=\\[)|(?<=\\])")
  }

  def userInfo(loginId: String) = {
    Config.getContextRoot() + loginId
  }

  def containsInDefaultMenus(menuName: String) = {
    val menus = play.Configuration.root.getString("project.creation.default.menus", "issue, milestone, board").replaceAll(" ", "").split(",")
    menus.toStream.contains(menuName)

  }
}
